import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Link as LinkIcon, Trash2, ExternalLink, Settings, LayoutTemplate } from "lucide-react";
import { Button, Card, CardTitle, Badge, Input, Label } from "@nexora/ui";
import { useAuth } from "@/features/auth/auth-context";
import { listBioPages, createBioPage, addBioEntry, removeBioEntry, updateBioPage } from "@/lib/api";
import type { LinkInBioPage, LinkInBioEntry } from "@nexora/contracts";

export function BioPageManager() {
  const { session } = useAuth();
  const queryClient = useQueryClient();
  const workspaceId = session?.currentWorkspace?.workspaceId ?? "";
  const accessToken = session?.session?.accessToken ?? "";

  const [selectedPage, setSelectedPage] = useState<LinkInBioPage | null>(null);

  const { data: pages = [], isLoading } = useQuery({
    queryKey: ["bio-pages", workspaceId],
    queryFn: () => listBioPages(workspaceId, accessToken),
    enabled: !!workspaceId && !!accessToken
  });

  const createMutation = useMutation({
    mutationFn: (slug: string) => createBioPage(workspaceId, accessToken, { slug, title: "My Bio Page" }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["bio-pages", workspaceId] });
    }
  });

  if (selectedPage) {
    return (
      <BioPageEditor 
        page={selectedPage} 
        onBack={() => setSelectedPage(null)} 
        workspaceId={workspaceId} 
        accessToken={accessToken} 
      />
    );
  }

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h2 className="font-display text-2xl text-slate-950">Link-in-Bio Pages</h2>
          <p className="mt-1 text-sm text-slate-500">Manage your micro-landing pages for Instagram and TikTok.</p>
        </div>
        <Button 
          className="shadow-lg shadow-sky-600/25 transition-all hover:scale-[1.03]"
          onClick={() => {
            const slug = prompt("Enter a unique slug for your page (e.g. my-brand):");
            if (slug) createMutation.mutate(slug);
          }}
          disabled={createMutation.isPending}
        >
          <Plus className="mr-2 h-4 w-4" /> Create Page
        </Button>
      </div>

      {isLoading ? (
        <div className="flex h-40 items-center justify-center">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-slate-200 border-t-sky-500" />
        </div>
      ) : pages.length === 0 ? (
        <Card className="flex flex-col items-center justify-center py-16 text-center border-dashed">
          <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-sky-50 text-sky-500 mb-4">
            <LayoutTemplate className="h-8 w-8" />
          </div>
          <h3 className="text-lg font-semibold text-slate-900">No pages yet</h3>
          <p className="mt-2 text-sm text-slate-500 max-w-sm">
            Create your first Link-in-Bio page to start driving traffic from your social profiles.
          </p>
        </Card>
      ) : (
        <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
          {pages.map(page => (
            <Card 
              key={page.pageId} 
              className="group cursor-pointer hover-lift flex flex-col justify-between"
              onClick={() => setSelectedPage(page)}
            >
              <div>
                <div className="flex items-start justify-between mb-4">
                  <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 text-white shadow-lg shadow-indigo-500/30">
                    <LinkIcon className="h-5 w-5" />
                  </div>
                  <Badge className={page.isActive ? "bg-emerald-100 text-emerald-700" : "bg-slate-100 text-slate-600"}>
                    {page.isActive ? "Active" : "Inactive"}
                  </Badge>
                </div>
                <CardTitle className="text-xl">{page.title}</CardTitle>
                <p className="mt-1 text-sm text-slate-500 flex items-center gap-1.5">
                  nexora.io/bio/{page.slug}
                </p>
              </div>
              <div className="mt-6 pt-4 border-t border-slate-100 flex items-center justify-between text-sm">
                <span className="text-slate-500 font-medium">{page.entries.length} Links</span>
                <span className="text-sky-600 font-semibold group-hover:underline">Manage &rarr;</span>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

// ── Editor Component ────────────────────────────────────────────────────────

function BioPageEditor({ 
  page, 
  onBack,
  workspaceId,
  accessToken
}: { 
  page: LinkInBioPage; 
  onBack: () => void;
  workspaceId: string;
  accessToken: string;
}) {
  const queryClient = useQueryClient();
  const [newLabel, setNewLabel] = useState("");
  const [newUrl, setNewUrl] = useState("");

  const addEntryMutation = useMutation({
    mutationFn: () => addBioEntry(workspaceId, page.pageId, accessToken, {
      label: newLabel,
      externalUrl: newUrl,
      sortOrder: page.entries.length,
      isPinned: false
    }),
    onSuccess: () => {
      setNewLabel("");
      setNewUrl("");
      queryClient.invalidateQueries({ queryKey: ["bio-pages", workspaceId] });
    }
  });

  const removeEntryMutation = useMutation({
    mutationFn: (entryId: string) => removeBioEntry(workspaceId, page.pageId, entryId, accessToken),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["bio-pages", workspaceId] });
    }
  });

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center gap-4">
        <Button variant="secondary" onClick={onBack}>&larr; Back</Button>
        <div>
          <h2 className="font-display text-2xl text-slate-950">{page.title}</h2>
          <a 
            href={`/bio/${page.slug}`} 
            target="_blank" 
            rel="noreferrer"
            className="text-sm text-sky-600 hover:underline flex items-center gap-1 mt-1"
          >
            nexora.io/bio/{page.slug} <ExternalLink className="h-3 w-3" />
          </a>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_400px]">
        {/* Left Col: Links Management */}
        <div className="space-y-6">
          <Card>
            <CardTitle className="mb-4">Add New Link</CardTitle>
            <div className="grid gap-4 sm:grid-cols-[1fr_1fr_auto] items-end">
              <div>
                <Label className="mb-1 block">Button Label</Label>
                <Input 
                  placeholder="e.g. Visit my Website" 
                  value={newLabel}
                  onChange={e => setNewLabel(e.target.value)}
                />
              </div>
              <div>
                <Label className="mb-1 block">URL</Label>
                <Input 
                  placeholder="https://..." 
                  value={newUrl}
                  onChange={e => setNewUrl(e.target.value)}
                />
              </div>
              <Button 
                onClick={() => addEntryMutation.mutate()}
                disabled={!newLabel || !newUrl || addEntryMutation.isPending}
              >
                Add Link
              </Button>
            </div>
          </Card>

          <div className="space-y-3">
            <h3 className="font-semibold text-slate-900 px-1">Your Links</h3>
            {page.entries.length === 0 ? (
              <div className="rounded-2xl border border-dashed p-8 text-center text-slate-500 text-sm">
                No links added yet. Add your first link above.
              </div>
            ) : (
              [...page.entries].sort((a, b) => a.sortOrder - b.sortOrder).map(entry => (
                <div key={entry.entryId} className="flex items-center justify-between rounded-2xl border bg-white p-4 shadow-sm transition hover:shadow-md">
                  <div className="min-w-0">
                    <p className="font-semibold text-slate-900 truncate">{entry.label}</p>
                    <a href={entry.externalUrl || "#"} target="_blank" rel="noreferrer" className="text-xs text-slate-500 hover:text-sky-600 hover:underline truncate block mt-0.5">
                      {entry.externalUrl || "Linked to draft post"}
                    </a>
                  </div>
                  <div className="flex items-center gap-2">
                    <button 
                      className="p-2 text-red-400 hover:bg-red-50 hover:text-red-600 rounded-lg transition-colors"
                      onClick={() => removeEntryMutation.mutate(entry.entryId)}
                      disabled={removeEntryMutation.isPending}
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Right Col: Preview / Settings */}
        <div className="space-y-6">
          <Card className="bg-slate-50 border-slate-200">
            <CardTitle className="mb-4 flex items-center gap-2 text-slate-700">
              <Settings className="h-4 w-4" /> Page Settings
            </CardTitle>
            <div className="space-y-4">
              <div>
                <Label className="mb-1 block text-slate-600">Page Title</Label>
                <Input defaultValue={page.title} readOnly className="bg-white" />
              </div>
              <div>
                <Label className="mb-1 block text-slate-600">Bio Text</Label>
                <Input defaultValue={page.bioText || ""} placeholder="Add a short bio..." readOnly className="bg-white" />
              </div>
              <p className="text-xs text-slate-500 mt-2">
                (Updating page settings requires additional UI implementation. For now, manage links.)
              </p>
            </div>
          </Card>
        </div>
      </div>
    </div>
  );
}
