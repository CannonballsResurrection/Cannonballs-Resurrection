import AppKit
import WebKit

/// In-app viewer for the original bundled Help! pages (Resources/HELP).
///
/// The 2002 game ran as an applet inside its host web page; the Help! menu
/// called out through that page's JavaScript bridge (Main.java:248
/// `HostPage.call("launchHelpPage", ...)`, Main.java:219 `launchTutorial`),
/// so the pages appeared in the same browser hosting the game. The native
/// app keeps them inside the game window instead of bouncing to an external
/// browser. Panel sizing is ours (interpolation, not source data): the pages
/// were authored for the 800x600 game host (content tables 610 px wide,
/// tutorial screenshots 800x600), so the content column is 800 px.
final class HelpViewer: NSView, WKNavigationDelegate {
    private static weak var current: HelpViewer?

    private let webView: WKWebView
    private var keyMonitor: Any?

    static func present(over host: NSView, file: String, fragment: String? = nil) {
        // Assets.url is base-relative when rooted at Bundle.resourceURL, and
        // URLComponents(resolvingAgainstBaseURL: false) drops the base, yielding
        // a scheme-less URL that WKWebView.loadFileURL rejects with
        // NSInvalidArgumentException. Resolve to an absolute URL first.
        var url = Assets.url("HELP/\(file)").absoluteURL
        if let fragment,
           var comps = URLComponents(url: url, resolvingAgainstBaseURL: false) {
            comps.fragment = fragment
            url = comps.url ?? url
        }
        if let open = current, open.superview === host {
            open.load(url)
            return
        }
        current?.dismiss()
        let viewer = HelpViewer(frame: host.bounds)
        viewer.autoresizingMask = [.width, .height]
        host.addSubview(viewer)
        current = viewer
        viewer.load(url)
        viewer.window?.makeFirstResponder(viewer.webView)
    }

    override init(frame: NSRect) {
        let conf = WKWebViewConfiguration()
        // The pages hard-code BODY BGCOLOR=#FFFFFF, but in the game they had no
        // backdrop of their own; only the panel art (images/bg_*.gif) should
        // paint. Override the body color at render time so the original .htm
        // assets stay untouched. Author CSS outranks the presentational
        // BGCOLOR hint, so this rule is enough.
        let clearBody = WKUserScript(
            source: """
            const s = document.createElement('style');
            s.textContent = 'body { background: transparent !important; }';
            document.documentElement.appendChild(s);
            """,
            injectionTime: .atDocumentStart,
            forMainFrameOnly: true)
        conf.userContentController.addUserScript(clearBody)
        webView = WKWebView(frame: .zero, configuration: conf)
        // macOS WKWebView has no public isOpaque/drawsBackground; KVC on
        // "drawsBackground" is the accepted way to get a transparent web view.
        webView.setValue(false, forKey: "drawsBackground")
        super.init(frame: frame)

        // dim the game behind the page, like a browser window taking focus
        wantsLayer = true
        layer?.backgroundColor = NSColor(white: 0, alpha: 0.6).cgColor

        webView.navigationDelegate = self
        addSubview(webView)

        let close = NSButton(title: "Close  [Esc]", target: self, action: #selector(closePressed))
        close.bezelStyle = .rounded
        close.frame.origin = .zero
        closeButton = close
        addSubview(close)

        layoutPanel()

        keyMonitor = NSEvent.addLocalMonitorForEvents(matching: .keyDown) { [weak self] e in
            guard let self, self.window === e.window else { return e }
            if e.keyCode == 53 { self.dismiss(); return nil }   // Esc
            return e
        }
    }
    required init?(coder: NSCoder) { fatalError("unused") }

    private var closeButton: NSButton?

    override func layout() {
        super.layout()
        layoutPanel()
    }

    private func layoutPanel() {
        // 800 px content column (the pages' authored width); fill the height.
        let w = min(bounds.width - 40, 800)
        let h = bounds.height - 56
        let x = (bounds.width - w) / 2
        webView.frame = NSRect(x: x, y: 8, width: w, height: h)
        if let close = closeButton {
            close.sizeToFit()
            close.frame.origin = NSPoint(x: x + w - close.frame.width,
                                         y: h + 8 + (40 - close.frame.height) / 2)
        }
    }

    private func load(_ url: URL) {
        // .path excludes any #fragment, so the read-access root stays a clean
        // directory URL (deletingLastPathComponent alone keeps the fragment).
        let helpRoot = URL(fileURLWithPath: url.deletingLastPathComponent().path,
                           isDirectory: true)
        webView.loadFileURL(url, allowingReadAccessTo: helpRoot)
    }

    @objc private func closePressed() { dismiss() }

    // swallow clicks on the dimmed backdrop; click outside the page closes it
    override func mouseDown(with event: NSEvent) {
        let p = convert(event.locationInWindow, from: nil)
        if !webView.frame.contains(p) { dismiss() }
    }

    func dismiss() {
        if let m = keyMonitor { NSEvent.removeMonitor(m); keyMonitor = nil }
        let host = superview
        removeFromSuperview()
        if let host { host.window?.makeFirstResponder(host) }
    }

    deinit { if let m = keyMonitor { NSEvent.removeMonitor(m) } }

    // Keep file/anchor navigation (tutorial1-5 arrows, #playing) inside the
    // panel; anything non-local (support links) goes to the real browser.
    func webView(_ webView: WKWebView,
                 decidePolicyFor navigationAction: WKNavigationAction,
                 decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        if let url = navigationAction.request.url, !url.isFileURL {
            NSWorkspace.shared.open(url)
            decisionHandler(.cancel)
            return
        }
        decisionHandler(.allow)
    }
}
