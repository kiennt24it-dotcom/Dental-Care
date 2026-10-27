import { describe, expect, it } from "vitest";
import { formatClinicTime, toClinicInstant } from "./clinicTime";

describe("clinic time", () => {
  it("converts API times with seconds to a Vietnam-time ISO instant", () => {
    expect(toClinicInstant("2026-10-07", "08:30:00")).toBe("2026-10-07T01:30:00.000Z");
  });

  it("accepts time values without seconds", () => {
    expect(toClinicInstant("2026-10-07", "08:30")).toBe("2026-10-07T01:30:00.000Z");
  });

  it("formats API slot times for display", () => {
    expect(formatClinicTime("08:30:00")).toBe("08:30");
  });

  it("rejects invalid clinic date/time values", () => {
    expect(() => toClinicInstant("not-a-date", "08:30:00")).toThrow(RangeError);
  });
});
