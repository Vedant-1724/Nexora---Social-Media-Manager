import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi } from 'vitest';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthProvider } from '@/features/auth/auth-context';
import { ComposerPage } from './composer-page';

// Mock dependencies safely
vi.mock('@nexora/ui', () => ({
  Button: ({ children }: any) => <button data-testid="ui-button">{children}</button>,
  Textarea: () => <textarea data-testid="ui-textarea" />,
  Card: ({ children }: any) => <div data-testid="ui-card">{children}</div>,
  CardTitle: ({ children }: any) => <div data-testid="ui-card-title">{children}</div>,
  Badge: ({ children }: any) => <div data-testid="ui-badge">{children}</div>,
  cn: (...args: any[]) => args.join(' '),
}));

vi.mock('@/lib/api', () => ({
  getSocialAccounts: vi.fn().mockResolvedValue([]),
  createDraft: vi.fn(),
  updateDraft: vi.fn(),
  scheduleDraft: vi.fn(),
}));

describe('ComposerPage', () => {
  it('renders the composer view correctly', () => {
    const queryClient = new QueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AuthProvider>
            <ComposerPage />
          </AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>
    );

    // Asserting ComposerPage header text
    expect(screen.getByText('Post Composer')).toBeInTheDocument();
  });
});

