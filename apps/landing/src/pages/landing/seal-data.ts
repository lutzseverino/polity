// Pre-rendered seal art. A clean SVG seal (concentric rings, a ring of small
// stars, and a jewelled eight-point star) was rasterised through a headless
// browser and sampled into a luminance ramp offline, then pasted here. To change
// the seal, re-run that conversion on a new source image/SVG and replace these
// rows. Rows are padded to SEAL_WIDTH at render; colour is assigned by radius.

export const SEAL_WIDTH = 53;

export const SEAL_LINES: readonly string[] = [
  "                    .+%@@@@@@@%+.",
  "               .%@=    -+***+-    =@%.",
  "            :@=  +=               =+  =@:",
  "          ##  #           *           #  ##",
  "        ** =.     .      .:       .     .= **",
  "      .@ -.       --             --       .- @.",
  "     *- +                                   + -*",
  "    %..     @.      %=         =%      .@     ..%",
  "   %.:           #.       @       .#           :.%",
  "  +-           *.         @         .*           -+",
  "  % =   =     %  :=      *@*      --  %     =   = %",
  " % +   .+    #    .@@.   @@@   .@@.    #    +.   + %",
  " @ -        %      .@@@%:@@@:#@@@.      %        - @",
  "--          :        @@@@@@@@@@@        :          --",
  "* -        +        -*@@#   #@@*-        +        - *",
  "* *   %*   * -@@@@@@@@@@ %@% @@@@@@@@@@: *   *%   * *",
  "* -        +        -*@@#   #@@*-        +        - *",
  "--          :        @@@@@@@@@@@        :          --",
  " @ -        %      .@@@#:@@@:%@@@.      %        - @",
  " % +   .#.   #    .@@.   @@@   .@@.    #   .#.   + %",
  "  % =  :      %  --      *@*      =:  %      :  = %",
  "  +-           *.         @         .*           -+",
  "   %.:           #.       @       .#           :.%",
  "    %..    -@*      %=         =%      *@-    ..%",
  "     *- +                                   + -*",
  "      .@ -.                               .- @.",
  "        ** =.     ##      #      ##     .= **",
  "          ##  #          . .          #  ##",
  "            :@=  +=               =+  =@:",
  "               .%@=    -+***+-    =@%.",
  "                    .+%@@@@@@@%+.",
];

export const SEAL_HEIGHT = SEAL_LINES.length;

/** A monospace cell is taller than it is wide; scaling x by this makes the
 *  round seal render round rather than stretched. Shared by every consumer so
 *  the geometry stays identical from a hero field down to a footer stamp. */
export const SEAL_CELL_ASPECT = 0.6;

// Inside this normalised radius the glyphs belong to the central star and burn
// red; everything outside (rings, the star-band, the rim) stays bone.
const STAR_RADIUS = 0.52;

const CX = (SEAL_WIDTH - 1) / 2;
const CY = (SEAL_HEIGHT - 1) / 2;
const MAX_R = (SEAL_HEIGHT - 1) / 2;

export type SealTone = "ring" | "star" | "space";

/** Normalised distance from the seal's centre — 0 at the core, ~1 at the rim. */
export function sealRadiusAt(x: number, y: number) {
  return Math.hypot((x - CX) * SEAL_CELL_ASPECT, y - CY) / MAX_R;
}

/** Classify a glyph by position: empty space, the red core star, or a bone ring. */
export function sealToneAt(ch: string, x: number, y: number): SealTone {
  if (ch === " ") {
    return "space";
  }
  return sealRadiusAt(x, y) < STAR_RADIUS ? "star" : "ring";
}
