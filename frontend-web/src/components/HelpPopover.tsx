import React, { useEffect, useRef } from "react";
import { Popover } from "bootstrap";

interface HelpPopoverProps {
  content: string;
  bootstrapColor?: string;
}

const HelpPopover: React.FC<HelpPopoverProps> = ({
  content,
  bootstrapColor,
}) => {
  const ref = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    if (ref.current) {
      new Popover(ref.current, {
        trigger: "click hover focus", // clic en móvil, hover en desktop
        content,
        placement: "right",
      });
    }
  }, [content]);

  const borderClass = bootstrapColor
    ? `border border-${bootstrapColor}`
    : "border";
  const textClass = bootstrapColor ? `text-${bootstrapColor}` : "text-muted";

  return (
    <span
      ref={ref}
      className={`${borderClass} ${textClass} ms-2 d-inline-flex justify-content-center align-items-center rounded-circle`}
      style={{ width: "1.5rem", height: "1.5rem", cursor: "pointer" }}
      role="button"
      tabIndex={0}
    >
      ?
    </span>
  );
};

export default HelpPopover;
