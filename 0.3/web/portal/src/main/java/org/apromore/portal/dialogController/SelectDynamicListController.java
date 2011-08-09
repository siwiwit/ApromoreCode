package org.apromore.portal.dialogController;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.zkoss.zk.ui.event.InputEvent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;


/**
 * source taken from ZK Small Talk "Autocomplete with Combobox"
 * @author fauvet
 *
 */
public class SelectDynamicListController extends Combobox {

	private MainController mainC ;
	private List<String> references;

	public SelectDynamicListController(List<String> references) {
		this.references = references;
		refresh(""); //init the child comboitems
	}

	public void setValue(String value) {
		super.setValue(value);
		refresh(value); //refresh the child comboitems
	}

	/** 
	 * Listens what an user is entering.
	 */
	public void onChanging(InputEvent evt) {
		if (!evt.isChangingBySelectBack())
			refresh(evt.getValue());
	}

	/** 
	 * Refreshes comboitem based on the specified value.
	 */
	public void refresh(String val) {

		int j = 0;
		while (j < this.references.size() 
				&& this.references.get(j).compareTo(val)<0) j++;

		Iterator<Comboitem> it = getItems().iterator();
		for (int cnt = 10; --cnt >= 0 && j < this.references.size() 
		&& this.references.get(j).startsWith(val); ++j) {
			if (it != null && it.hasNext()) {
				it.next().setLabel(this.references.get(j));
			} else {
				it = null;
				new Comboitem(this.references.get(j)).setParent(this);
			}
		}

		while (it != null && it.hasNext()) {
			it.next();
			it.remove();
		}
	}

	public String getValue() {
		String value = super.getValue();
		return value;

	}

	public List<String> getReferences() {
		if (this.references == null) {
			this.references = new ArrayList<String>();
		}
		return this.references;
	}

	public void setReference(List<String> ref) {
		this.references = ref;
	}

	public void addItem(String item) {
		// insert an item. Keep the list ordered, with elements pairwise distinct
		int i = 0;
		while (i < this.references.size() && this.references.get(i).compareTo(item) < 0) i++;
		if (i == this.references.size()) {
			// query is the greatest
			this.references.add(item);
		} else {
			if (this.references.get(i).compareTo(item) > 0) {
				// not found. Smaller than this.searchHist.get(i)
				this.references.add(i, item);
			}
		}

	}

}