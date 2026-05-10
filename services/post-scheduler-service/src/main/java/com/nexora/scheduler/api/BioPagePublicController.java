package com.nexora.scheduler.api;

import com.nexora.scheduler.service.PostSchedulerService;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BioPagePublicController {

  private final PostSchedulerService postSchedulerService;

  public BioPagePublicController(PostSchedulerService postSchedulerService) {
    this.postSchedulerService = postSchedulerService;
  }

  @GetMapping(value = "/bio/{slug}", produces = MediaType.TEXT_HTML_VALUE)
  public String renderBioPage(@PathVariable("slug") String slug) {
    PostSchedulerService.PublicBioPageView page = postSchedulerService.getPublicBioPage(slug);

    String entriesHtml = page.entries().stream()
        .map(entry -> {
          String thumb = entry.thumbnailUrl() != null && !entry.thumbnailUrl().isBlank()
              ? "<img src=\"" + escapeHtml(entry.thumbnailUrl()) + "\" alt=\"\" class=\"entry-thumb\"/>"
              : "<div class=\"entry-thumb entry-thumb-placeholder\"></div>";
          String url = entry.externalUrl() != null && !entry.externalUrl().isBlank()
              ? entry.externalUrl() : "#";
          String pin = entry.isPinned() ? "<span class=\"pin-badge\">📌</span>" : "";
          return """
              <a href="%s" class="bio-entry" target="_blank" rel="noopener noreferrer">
                %s
                <span class="entry-label">%s%s</span>
                <svg class="arrow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 12h14m-7-7 7 7-7 7"/></svg>
              </a>
              """.formatted(escapeHtml(url), thumb, pin, escapeHtml(entry.label()));
        })
        .collect(Collectors.joining("\n"));

    String avatarHtml = page.avatarUrl() != null && !page.avatarUrl().isBlank()
        ? "<img src=\"" + escapeHtml(page.avatarUrl()) + "\" alt=\"" + escapeHtml(page.title()) + "\" class=\"avatar\"/>"
        : "<div class=\"avatar avatar-placeholder\">" + escapeHtml(page.title().substring(0, 1).toUpperCase()) + "</div>";

    String bioTextHtml = page.bioText() != null && !page.bioText().isBlank()
        ? "<p class=\"bio-text\">" + escapeHtml(page.bioText()) + "</p>" : "";

    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8"/>
          <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
          <title>%s — Link in Bio</title>
          <meta name="description" content="%s"/>
          <style>
            *,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
            body{
              min-height:100vh;
              font-family:'Inter','Segoe UI',system-ui,-apple-system,sans-serif;
              background:linear-gradient(135deg,#0f0c29 0%%,#302b63 50%%,#24243e 100%%);
              color:#e2e8f0;
              display:flex;justify-content:center;align-items:flex-start;
              padding:2rem 1rem;
            }
            .container{
              width:100%%;max-width:420px;
              display:flex;flex-direction:column;align-items:center;gap:1.5rem;
            }
            .avatar{
              width:96px;height:96px;border-radius:50%%;
              object-fit:cover;
              border:3px solid rgba(255,255,255,0.2);
              box-shadow:0 0 30px rgba(139,92,246,0.3);
            }
            .avatar-placeholder{
              display:flex;align-items:center;justify-content:center;
              font-size:2rem;font-weight:700;
              background:linear-gradient(135deg,#8b5cf6,#06b6d4);
              color:#fff;
            }
            .page-title{font-size:1.5rem;font-weight:700;text-align:center}
            .bio-text{font-size:0.875rem;text-align:center;color:#94a3b8;max-width:320px;line-height:1.6}
            .entries{display:flex;flex-direction:column;gap:0.75rem;width:100%%}
            .bio-entry{
              display:flex;align-items:center;gap:0.75rem;
              padding:0.875rem 1rem;
              background:rgba(255,255,255,0.08);
              backdrop-filter:blur(12px);
              border:1px solid rgba(255,255,255,0.1);
              border-radius:16px;
              text-decoration:none;color:#e2e8f0;
              transition:all 0.25s ease;
              cursor:pointer;
            }
            .bio-entry:hover{
              background:rgba(255,255,255,0.14);
              border-color:rgba(139,92,246,0.4);
              transform:translateY(-2px);
              box-shadow:0 8px 24px rgba(0,0,0,0.3);
            }
            .entry-thumb{width:44px;height:44px;border-radius:12px;object-fit:cover;flex-shrink:0}
            .entry-thumb-placeholder{
              background:linear-gradient(135deg,#8b5cf6 0%%,#06b6d4 100%%);
              opacity:0.4;
            }
            .entry-label{flex:1;font-weight:500;font-size:0.9rem}
            .pin-badge{margin-right:0.25rem}
            .arrow-icon{width:18px;height:18px;flex-shrink:0;opacity:0.4}
            .bio-entry:hover .arrow-icon{opacity:0.8}
            .footer{
              margin-top:2rem;font-size:0.75rem;color:#475569;
              text-align:center;
            }
            .footer a{color:#8b5cf6;text-decoration:none}
            .footer a:hover{text-decoration:underline}
          </style>
        </head>
        <body>
          <div class="container">
            %s
            <h1 class="page-title">%s</h1>
            %s
            <div class="entries">
              %s
            </div>
            <div class="footer">
              Powered by <a href="#">Nexora</a>
            </div>
          </div>
        </body>
        </html>
        """.formatted(
        escapeHtml(page.title()),
        escapeHtml(page.bioText() != null ? page.bioText() : page.title()),
        avatarHtml,
        escapeHtml(page.title()),
        bioTextHtml,
        entriesHtml);
  }

  private String escapeHtml(String input) {
    if (input == null) return "";
    return input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
