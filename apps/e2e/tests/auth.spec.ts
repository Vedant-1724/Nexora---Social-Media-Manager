import { test, expect } from '@playwright/test';

test.describe('Authentication Flows', () => {
  test('should render the login page correctly', async ({ page }) => {
    // Navigate to the frontend login page
    await page.goto('/auth/login');

    // Check if critical elements are visible
    await expect(page.getByRole('heading', { name: /Sign in to your workspace/i })).toBeVisible();
    await expect(page.getByLabel(/Work Email/i)).toBeVisible();
    await expect(page.getByLabel(/Password/i)).toBeVisible();
    await expect(page.getByRole('button', { name: /Sign in/i })).toBeVisible();
  });

  test('should redirect unauthenticated user to login', async ({ page }) => {
    // Attempting to access protected dashboard route directly
    await page.goto('/app/dashboard');

    // Should automatically redirect to login with the correct query param
    await expect(page).toHaveURL(/\/auth\/login\?next=.*app%2Fdashboard/);
    await expect(page.getByRole('heading', { name: /Sign in to your workspace/i })).toBeVisible();
  });
});
