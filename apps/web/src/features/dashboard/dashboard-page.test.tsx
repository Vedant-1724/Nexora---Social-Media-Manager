import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { vi } from "vitest";

import { AuthProvider } from "@/features/auth/auth-context";
import { DashboardPage } from "./dashboard-page";

// Mock the API calls for the dashboard
vi.mock("@/lib/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api")>();
  return {
    ...actual,
    getAnalyticsOverview: vi.fn(),
  };
});

describe("DashboardPage", () => {
  function renderDashboard() {
    const queryClient = new QueryClient();
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AuthProvider>
            <DashboardPage />
          </AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  it("renders the dashboard greeting and stat cards", () => {
    renderDashboard();

    expect(screen.getByText(/Welcome back/i)).toBeInTheDocument();
    expect(screen.getByText(/Scheduled Posts/i)).toBeInTheDocument();
    expect(screen.getByText(/Weekly Engagements/i)).toBeInTheDocument();
    expect(screen.getByText(/New Followers/i)).toBeInTheDocument();
  });

  it("displays the upcoming posts and activity feeds", () => {
    renderDashboard();

    expect(screen.getByText(/Upcoming Posts/i)).toBeInTheDocument();
    expect(screen.getByText(/Activity/i)).toBeInTheDocument();
  });
});
