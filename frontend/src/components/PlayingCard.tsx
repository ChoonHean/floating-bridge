type PlayingCardProps = {
  code: string;
  width?: number;
  selected: boolean;
  onClick?: () => void;
};

export default function PlayingCard({
  code,
  width = 80,
  selected = false,
  onClick,
}: PlayingCardProps) {
  return (
    <img
      src={`/cards/${code}.svg`}
      alt={code}
      width={width}
      draggable={false}
      onClick={onClick}
      style={{
        cursor: onClick ? "pointer" : "default",
        userSelect: "none",
        transform: selected ? "translateY(-16px)" : "translateY(0)",
        transition: "transform 0.15s ease-out",
      }}
    />
  );
}