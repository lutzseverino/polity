import { type ComponentProps, useState } from "react";

import { Input } from "@/components/ui/input";
import { cn } from "@/lib/utils";

type AppInputProps = Readonly<
  ComponentProps<"input"> & {
    animatedPlaceholder?: string;
  }
>;

export function AppInput({
  animatedPlaceholder,
  className,
  defaultValue,
  onChange,
  placeholder,
  value,
  ...props
}: AppInputProps) {
  const [uncontrolledValue, setUncontrolledValue] = useState(defaultValue);
  const currentValue = value !== undefined ? value : uncontrolledValue;
  const displayedPlaceholder = animatedPlaceholder ?? placeholder;
  const hasValue =
    currentValue !== undefined &&
    currentValue !== null &&
    String(currentValue).length > 0;
  const showAnimatedPlaceholder = Boolean(animatedPlaceholder && !hasValue);
  const handleChange: ComponentProps<"input">["onChange"] = (event) => {
    if (value === undefined) {
      setUncontrolledValue(event.target.value);
    }
    onChange?.(event);
  };
  const inputClassName = cn(
    "h-11 rounded-none border-2 text-base",
    showAnimatedPlaceholder && "placeholder:text-transparent",
    className,
  );

  if (!animatedPlaceholder) {
    return (
      <Input
        className={inputClassName}
        defaultValue={defaultValue}
        onChange={handleChange}
        placeholder={displayedPlaceholder}
        value={value}
        {...props}
      />
    );
  }

  return (
    <span className="relative block min-w-0">
      <Input
        className={inputClassName}
        defaultValue={defaultValue}
        onChange={handleChange}
        placeholder={displayedPlaceholder}
        value={value}
        {...props}
      />
      {showAnimatedPlaceholder ? (
        <span
          aria-hidden="true"
          className="pointer-events-none absolute inset-y-0 left-2.5 right-2.5 flex items-center truncate text-base text-muted-foreground md:text-sm motion-safe:animate-in motion-safe:fade-in-0 motion-safe:slide-in-from-bottom-1 motion-safe:duration-700"
          key={animatedPlaceholder}
        >
          {animatedPlaceholder}
        </span>
      ) : null}
    </span>
  );
}
