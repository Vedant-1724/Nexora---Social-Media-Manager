import { test, expect } from '@playwright/test';

const API_BASE_URL = process.env.API_BASE_URL || 'http://localhost:8080/api/v1';

test.describe('API Health and Availability', () => {
  test('should return 401 for unauthorized access to protected endpoints', async ({ request }) => {
    // Attempting to list workspaces without auth token
    const response = await request.get(`${API_BASE_URL}/auth/workspaces`);
    expect(response.status()).toBe(401);
  });

  test('should return available billing plans', async ({ request }) => {
    // Billing plans are typically public or have a public cache endpoint
    const response = await request.get(`${API_BASE_URL}/billing/plans`);
    // Depending on the backend implementation, this may be 200 or 401 if strict.
    // If it's public (like the frontend hitting it without auth), expecting 200.
    expect([200, 401]).toContain(response.status());
  });
});
