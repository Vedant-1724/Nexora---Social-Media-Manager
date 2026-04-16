import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { vi } from "vitest";

import { AuthProvider } from "@/features/auth/auth-context";
import { AnalyticsPage } from "./analytics-page";

// Mock the API calls for the analytics page
vi.mock("@/lib/api", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/lib/api")>();
  return {
    ...actual,
    getAnalyticsOverview: vi.fn(),
    getAnalyticsTimeSeries: vi.fn(),
    getTopContent: vi.fn(),
    getPlatformBreakdown: vi.fn(),
  };
});

describe("AnalyticsPage", () => {
  function renderAnalytics() {
    const queryClient = new QueryClient();
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AuthProvider>
            <AnalyticsPage />
          </AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  it("renders the analytics header and metric cards", () => {
    renderAnalytics();

    expect(screen.getByText("Analytics")).toBeInTheDocument();
    expect(screen.getByText(/Total Impressions/i)).toBeInTheDocument();
  });

  it("changes the select time range accurately", () => {
    renderAnalytics();

    const range30D = screen.getByRole("button", { name: "30D" });
    const range90D = screen.getByRole("button", { name: "90D" });

    expect(range30D).toBeInTheDocument();
    expect(range90D).toBeInTheDocument();

    fireEvent.click(range30D);
    expect(range30D).toHaveClass("bg-slate-950");
  });
});
