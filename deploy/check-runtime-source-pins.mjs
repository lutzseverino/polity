#!/usr/bin/env node

import { readdir, readFile, stat } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const DOCKERFILE_SUFFIX = "Dockerfile";
const SUPPORTED_INSTRUCTIONS = new Set([
  "ADD",
  "ARG",
  "CMD",
  "COPY",
  "ENTRYPOINT",
  "ENV",
  "EXPOSE",
  "FROM",
  "HEALTHCHECK",
  "LABEL",
  "MAINTAINER",
  "ONBUILD",
  "RUN",
  "SHELL",
  "STOPSIGNAL",
  "USER",
  "VOLUME",
  "WORKDIR",
]);

class DockerfilePolicyError extends Error {
  constructor(path, line, message) {
    super(`${path}:${line}: ${message}`);
  }
}

async function collectDockerfiles(path) {
  const details = await stat(path);
  if (details.isFile()) {
    return [path];
  }
  if (!details.isDirectory()) {
    throw new Error(`${path}: expected a file or directory`);
  }

  const files = [];
  const entries = await readdir(path, { withFileTypes: true });
  for (const entry of entries.sort((left, right) => left.name.localeCompare(right.name))) {
    const child = resolve(path, entry.name);
    if (entry.isDirectory()) {
      files.push(...(await collectDockerfiles(child)));
    } else if (entry.isFile() && entry.name.endsWith(DOCKERFILE_SUFFIX)) {
      files.push(child);
    }
  }
  return files;
}

function logicalInstructions(path, content) {
  if (content.includes("\0")) {
    throw new DockerfilePolicyError(path, 1, "NUL bytes are unsupported");
  }

  const instructions = [];
  const physicalLines = content.split("\n");
  let logical = "";
  let startLine = 0;

  for (let index = 0; index < physicalLines.length; index += 1) {
    const lineNumber = index + 1;
    const physical = physicalLines[index].replace(/\r$/, "");

    if (!logical && /^\s*(?:#.*)?$/.test(physical)) {
      if (/^\s*#\s*syntax\s*=/i.test(physical)) {
        throw new DockerfilePolicyError(
          path,
          lineNumber,
          "custom Dockerfile syntax frontends are unsupported",
        );
      }
      const escapeDirective = physical.match(/^\s*#\s*escape\s*=\s*(\S+)\s*$/i);
      if (escapeDirective && escapeDirective[1] !== "\\") {
        throw new DockerfilePolicyError(
          path,
          lineNumber,
          "only backslash Dockerfile continuations are supported",
        );
      }
      continue;
    }
    if (logical && /^\s*#/.test(physical)) {
      continue;
    }

    if (!logical) {
      startLine = lineNumber;
    }
    const continues = physical.endsWith("\\");
    logical += continues ? physical.slice(0, -1) : physical;
    if (!continues) {
      instructions.push({ line: startLine, value: logical });
      logical = "";
    }
  }

  if (logical) {
    throw new DockerfilePolicyError(path, startLine, "unterminated Dockerfile continuation");
  }
  return instructions;
}

function parseInstruction(path, instruction) {
  const match = instruction.value.match(/^\s*([A-Za-z]+)(?:\s+|$)(.*)$/);
  if (!match) {
    throw new DockerfilePolicyError(
      path,
      instruction.line,
      "unsupported Dockerfile instruction syntax",
    );
  }

  const keyword = match[1].toUpperCase();
  if (!SUPPORTED_INSTRUCTIONS.has(keyword)) {
    throw new DockerfilePolicyError(
      path,
      instruction.line,
      `unsupported Dockerfile instruction: ${match[1]}`,
    );
  }
  if (match[2].includes("<<")) {
    throw new DockerfilePolicyError(path, instruction.line, "Dockerfile heredocs are unsupported");
  }
  return { keyword, line: instruction.line, value: match[2].trim() };
}

function parseFrom(path, instruction) {
  if (!instruction.value) {
    throw new DockerfilePolicyError(path, instruction.line, "FROM has no source image");
  }
  if (instruction.value.includes("$")) {
    throw new DockerfilePolicyError(
      path,
      instruction.line,
      "dynamic FROM expressions are unsupported",
    );
  }

  const fields = instruction.value.split(/\s+/);
  let sourceIndex = 0;
  while (fields[sourceIndex]?.startsWith("--")) {
    if (!/^--platform=[A-Za-z0-9][A-Za-z0-9_./-]*$/.test(fields[sourceIndex])) {
      throw new DockerfilePolicyError(
        path,
        instruction.line,
        `unsupported FROM flag: ${fields[sourceIndex]}`,
      );
    }
    sourceIndex += 1;
  }

  const source = fields[sourceIndex];
  if (!source) {
    throw new DockerfilePolicyError(path, instruction.line, "FROM has no source image");
  }

  const trailing = fields.slice(sourceIndex + 1);
  let alias;
  if (trailing.length > 0) {
    if (
      trailing.length !== 2 ||
      trailing[0].toUpperCase() !== "AS" ||
      !/^[A-Za-z0-9][A-Za-z0-9_.-]*$/.test(trailing[1])
    ) {
      throw new DockerfilePolicyError(
        path,
        instruction.line,
        "FROM must contain only flags, one source, and an optional AS stage alias",
      );
    }
    alias = trailing[1];
  }
  return { alias, source };
}

function assertExternalSourcePinned(path, line, source) {
  const digestSeparator = source.indexOf("@sha256:");
  if (digestSeparator < 0 || source.indexOf("@", digestSeparator + 1) >= 0) {
    throw new DockerfilePolicyError(
      path,
      line,
      `external FROM source must include a sha256 digest: ${source}`,
    );
  }

  const imageAndTag = source.slice(0, digestSeparator);
  const digest = source.slice(digestSeparator + "@sha256:".length);
  const lastSlash = imageAndTag.lastIndexOf("/");
  const tagSeparator = imageAndTag.lastIndexOf(":");
  const tag = tagSeparator > lastSlash ? imageAndTag.slice(tagSeparator + 1) : "";
  if (
    !/^[A-Za-z0-9_][A-Za-z0-9_.-]{0,127}$/.test(tag) ||
    tag.toLowerCase() === "latest"
  ) {
    throw new DockerfilePolicyError(
      path,
      line,
      `external FROM source must use a readable, non-latest tag: ${source}`,
    );
  }
  if (digest.length !== 64) {
    throw new DockerfilePolicyError(
      path,
      line,
      `external FROM source must use a full sha256 digest: ${source}`,
    );
  }
  if (!/^[0-9a-f]+$/.test(digest)) {
    throw new DockerfilePolicyError(
      path,
      line,
      `external FROM source must use a lowercase hexadecimal sha256 digest: ${source}`,
    );
  }
}

async function checkDockerfile(path) {
  const content = await readFile(path, "utf8");
  const aliases = new Set();
  let fromCount = 0;

  for (const logical of logicalInstructions(path, content)) {
    const instruction = parseInstruction(path, logical);
    if (instruction.keyword !== "FROM") {
      continue;
    }

    fromCount += 1;
    const { alias, source } = parseFrom(path, instruction);
    const normalizedSource = source.toLowerCase();
    if (normalizedSource !== "scratch" && !aliases.has(normalizedSource)) {
      assertExternalSourcePinned(path, instruction.line, source);
    }
    if (alias) {
      const normalizedAlias = alias.toLowerCase();
      if (aliases.has(normalizedAlias)) {
        throw new DockerfilePolicyError(
          path,
          instruction.line,
          `duplicate FROM stage alias: ${alias}`,
        );
      }
      aliases.add(normalizedAlias);
    }
  }

  if (fromCount === 0) {
    throw new DockerfilePolicyError(path, 1, "Dockerfile contains no FROM instruction");
  }
}

async function main() {
  const scriptDirectory = dirname(fileURLToPath(import.meta.url));
  const requestedPaths =
    process.argv.length > 2 ? process.argv.slice(2).map((path) => resolve(path)) : [scriptDirectory];
  const dockerfiles = [
    ...new Set(
      (
        await Promise.all(requestedPaths.map((path) => collectDockerfiles(path)))
      ).flat(),
    ),
  ].sort();

  if (dockerfiles.length === 0) {
    throw new Error("No runtime Dockerfiles were found");
  }

  let failed = false;
  for (const dockerfile of dockerfiles) {
    try {
      await checkDockerfile(dockerfile);
    } catch (error) {
      failed = true;
      console.error(error instanceof Error ? error.message : error);
    }
  }
  if (failed) {
    process.exitCode = 1;
  }
}

await main();
