package mj.ocraptor.extraction;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class JSoupTools {

	/**
	 *
	 *
	 * @param doc
	 */
	public static void removeEmptyTags(Document doc) {
		if (doc != null) {
			for (Element element : doc.select("*")) {
				if (!element.hasText() && element.isBlock()) {
					element.remove();
				}
			}
		}
	}
}
