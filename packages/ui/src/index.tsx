import * as React from "react";
import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";
import { cva, type VariantProps } from "class-variance-authority";

// ── Utilities ───────────────────────────────────────────────────────────────

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// ── Badge ───────────────────────────────────────────────────────────────────

export function Badge({
  className,
  ...props
}: React.HTMLAttributes<HTMLSpanElement>) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full glass-light border-sky-500/30 px-3 py-1 text-xs font-semibold uppercase tracking-[0.24em] text-sky-300",
        className
      )}
      {...props}
    />
  );
}

// ── Button ──────────────────────────────────────────────────────────────────

const buttonVariants = cva(
  "inline-flex items-center justify-center rounded-full text-sm font-semibold transition duration-300 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-60",
  {
    variants: {
      variant: {
        primary:
          "glass-dark border border-sky-500/30 px-5 py-3 text-white shadow-[0_0_15px_rgba(14,165,233,0.3)] hover:border-sky-400/60 hover:shadow-[0_0_25px_rgba(14,165,233,0.5)] focus-visible:ring-sky-500",
        secondary:
          "glass-light border border-white/10 px-5 py-3 text-slate-200 hover:border-white/20 hover:bg-white/10 focus-visible:ring-slate-400",
        ghost: "px-4 py-2 text-slate-300 hover:bg-white/10 focus-visible:ring-slate-400"
      }
    },
    defaultVariants: {
      variant: "primary"
    }
  }
);

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement>,
    VariantProps<typeof buttonVariants> {}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant, ...props }, ref) => (
    <button
      className={cn(buttonVariants({ variant }), className)}
      ref={ref}
      {...props}
    />
  )
);

Button.displayName = "Button";

// ── Card ────────────────────────────────────────────────────────────────────

export function Card({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        "glass rounded-[28px] p-6",
        className
      )}
      {...props}
    />
  );
}

export function CardTitle({
  className,
  ...props
}: React.HTMLAttributes<HTMLHeadingElement>) {
  return (
    <h3
      className={cn("font-display text-xl font-semibold text-white", className)}
      {...props}
    />
  );
}

export function CardDescription({
  className,
  ...props
}: React.HTMLAttributes<HTMLParagraphElement>) {
  return (
    <p className={cn("text-sm leading-6 text-slate-400", className)} {...props} />
  );
}
