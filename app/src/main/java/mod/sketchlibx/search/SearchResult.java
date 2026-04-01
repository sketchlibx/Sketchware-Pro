package mod.sketchlibx.search;

public class SearchResult {
    public String fileName;    // XML or Java file name
    public String category;    // "View", "Component", "Variable", "Logic"
    public String title;       // e.g., "textview1 (TextView)"
    public String description; // The exact match text
    public int tabIndex;       // 0 = View, 1 = Event, 2 = Component

    public SearchResult(String fileName, String category, String title, String description, int tabIndex) {
        this.fileName = fileName;
        this.category = category;
        this.title = title;
        this.description = description;
        this.tabIndex = tabIndex;
    }
}
