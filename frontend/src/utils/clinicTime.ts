export function toClinicInstant(date: string, time: string): string {
  const normalizedTime = time.length === 5 ? `${time}:00` : time;
  const instant = new Date(`${date}T${normalizedTime}+07:00`);
  if (Number.isNaN(instant.getTime())) {
    throw new RangeError("Invalid clinic date or time");
  }
  return instant.toISOString();
}

export function formatClinicTime(time: string): string {
  return time.slice(0, 5);
}
