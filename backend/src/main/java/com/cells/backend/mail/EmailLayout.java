package com.cells.backend.mail;

import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * The one email look, built from the CELLS Design System Guide: navy header with the wordmark, white card,
 * gold primary button with navy text, muted small print. All text is escaped. Inline styles only,
 * because mail apps ignore stylesheets.
 */
public final class EmailLayout {

    private static final String NAVY = "#0E2250";
    private static final String GOLD = "#D4AF37";
    private static final String MUTED = "#47557A";
    private static final String BORDER = "#DCE3F2";
    private static final String APP_BG = "#F4F6FB";
    private static final String CLOSING = "Bringing Every Cell Group Together.";

    private EmailLayout() {
    }

    public static String html(String heading, List<String> paragraphs, String buttonLabel, String buttonUrl, String footnote) {
        StringBuilder body = new StringBuilder();
        for (String paragraph : paragraphs) {
            body.append("<p style=\"margin:0 0 14px;font-size:16px;line-height:1.5;color:").append(NAVY).append(";\">")
                    .append(esc(paragraph)).append("</p>");
        }
        String button = "";
        String fallback = "";
        if (buttonUrl != null) {
            button = "<p style=\"margin:22px 0 18px;\"><a href=\"" + esc(buttonUrl)
                    + "\" style=\"display:inline-block;background:" + GOLD + ";color:" + NAVY
                    + ";font-weight:700;font-size:16px;text-decoration:none;padding:14px 26px;border-radius:12px;\">"
                    + esc(buttonLabel) + "</a></p>";
            fallback = "<p style=\"margin:0 0 14px;font-size:13px;line-height:1.5;color:" + MUTED
                    + ";\">If the button doesn’t work, copy this link into your browser:<br>"
                    + "<span style=\"word-break:break-all;\">" + esc(buttonUrl) + "</span></p>";
        }
        String note = footnote == null ? "" : "<p style=\"margin:0 0 6px;font-size:13px;line-height:1.5;color:"
                + MUTED + ";\">" + esc(footnote) + "</p>";

        return "<!doctype html><html lang=\"en\"><body style=\"margin:0;padding:0;background:" + APP_BG
                + ";font-family:Inter,Arial,Helvetica,sans-serif;color:" + NAVY + ";\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:"
                + APP_BG + ";padding:24px 12px;\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"560\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:560px;width:100%;\">"
                + "<tr><td style=\"background:" + NAVY + ";border-radius:18px 18px 0 0;padding:22px 28px;\">"
                + "<span style=\"font-family:Montserrat,Arial,sans-serif;font-weight:800;font-size:20px;letter-spacing:4px;color:#FFFFFF;\">CELLS</span>"
                + "</td></tr>"
                + "<tr><td style=\"background:#FFFFFF;border:1px solid " + BORDER
                + ";border-top:none;border-radius:0 0 18px 18px;padding:28px;\">"
                + "<h1 style=\"margin:0 0 16px;font-family:Montserrat,Arial,sans-serif;font-size:22px;line-height:1.3;color:"
                + NAVY + ";\">" + esc(heading) + "</h1>"
                + body + button + fallback + note
                + "<p style=\"margin:14px 0 0;font-family:Montserrat,Arial,sans-serif;font-size:13px;font-weight:700;color:"
                + MUTED + ";\">" + CLOSING + "</p>"
                + "</td></tr></table></td></tr></table></body></html>";
    }

    public static String text(String heading, List<String> paragraphs, String buttonLabel, String buttonUrl, String footnote) {
        StringBuilder out = new StringBuilder("CELLS\n\n").append(heading).append("\n\n");
        for (String paragraph : paragraphs) {
            out.append(paragraph).append("\n\n");
        }
        if (buttonUrl != null) {
            out.append(buttonLabel).append(":\n").append(buttonUrl).append("\n\n");
        }
        if (footnote != null) {
            out.append(footnote).append("\n\n");
        }
        return out.append(CLOSING).append("\n").toString();
    }

    private static String esc(String value) {
        return HtmlUtils.htmlEscape(value);
    }
}
