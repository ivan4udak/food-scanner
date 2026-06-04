export function Spinner() {
  return <div className="ring" role="status" aria-label="Загрузка" />;
}

export function FullScreenSpinner() {
  return (
    <div className="page center" style={{ alignItems: 'center', justifyContent: 'center' }}>
      <Spinner />
    </div>
  );
}
