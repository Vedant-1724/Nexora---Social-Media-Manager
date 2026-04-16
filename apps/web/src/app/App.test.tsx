import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";

import { AuthProvider } from "@/features/auth/auth-context";
import { App } from "./App";

describe("App", () => {
  function renderApplication(initialEntries: string[]) {
    const queryClient = new QueryClient();

    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={initialEntries}>
          <AuthProvider>
            <App />
          </AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  it("renders the landing page headline", () => {
    renderApplication(["/"]);

    expect(
      screen.getByText(/for modern social teams/i)
    ).toBeInTheDocument();
  });

  it("redirects protected routes to the login screen", () => {
    renderApplication(["/app/dashboard"]);

    expect(screen.getByText(/Sign in to your workspace/i)).toBeInTheDocument();
  });
});
