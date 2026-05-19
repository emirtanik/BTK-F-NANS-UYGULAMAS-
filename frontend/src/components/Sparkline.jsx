const Sparkline = ({ data = [], positive = true, width = 120, height = 48 }) => {
  if (!data || data.length < 2) {
    return (
      <svg width={width} height={height}>
        <line x1="0" y1={height / 2} x2={width} y2={height / 2}
          stroke="#2a2a2a" strokeWidth="1" strokeDasharray="3 3" />
      </svg>
    );
  }

  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min || 1;
  const stepX = width / (data.length - 1);

  const points = data.map((v, i) => {
    const x = i * stepX;
    const y = height - ((v - min) / range) * (height - 4) - 2;
    return [x, y];
  });

  const pathD = points.map((p, i) => (i === 0 ? `M${p[0]},${p[1]}` : `L${p[0]},${p[1]}`)).join(' ');
  const areaD = `${pathD} L${width},${height} L0,${height} Z`;

  const color = positive ? '#22c55e' : '#ef4444';
  const fillId = `spark-fill-${Math.random().toString(36).slice(2, 8)}`;
  const baseY = points[0][1];

  return (
    <svg width={width} height={height}>
      <defs>
        <linearGradient id={fillId} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={color} stopOpacity="0.3" />
          <stop offset="100%" stopColor={color} stopOpacity="0" />
        </linearGradient>
      </defs>
      <line x1="0" y1={baseY} x2={width} y2={baseY}
        stroke={color} strokeWidth="1" strokeDasharray="3 3" opacity="0.4" />
      <path d={areaD} fill={`url(#${fillId})`} />
      <path d={pathD} fill="none" stroke={color} strokeWidth="1.5"
        strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
};

export default Sparkline;
