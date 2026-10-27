// @vitest-environment jsdom
import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { AuthProvider } from "../features/auth/AuthContext";
import { ProtectedRoute } from "./ProtectedRoute";

describe("ProtectedRoute", () => {
  it("redirects unauthenticated visitors to the login page", () => {
    localStorage.clear();
    render(<MemoryRouter initialEntries={["/app"]}><AuthProvider><Routes>
      <Route element={<ProtectedRoute />}><Route path="/app" element={<span>Private content</span>} /></Route>
      <Route path="/login" element={<span>Login page</span>} />
    </Routes></AuthProvider></MemoryRouter>);
    expect(screen.getByText("Login page")).toBeTruthy();
  });
});
