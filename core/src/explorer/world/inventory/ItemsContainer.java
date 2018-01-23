package explorer.world.inventory;

import com.badlogic.gdx.utils.Array;

public class ItemsContainer {
	
	public static class ItemsStack {
		private Item item;
		private int in_stack;
		
		private ItemsContainer parent;
		
		public ItemsStack(Item item, int in_stack, ItemsContainer parent) {
			this.parent = parent;
			this.item = item;
			this.in_stack = in_stack;
		}

		public ItemsStack(ItemsStack to_clone) {
			this.parent = to_clone.parent;
			this.item = to_clone.item;
			this.in_stack = to_clone.in_stack;
		}

		public void set(ItemsStack other) {
			this.parent = other.parent;
			this.item = other.item;
			this.in_stack = other.in_stack;
		}
		
		public ItemsContainer getParent() {
			return parent;
		}

		public void setParent(ItemsContainer parent) {
			this.parent = parent;
		}
		
		public Item getItem() {
			return item;
		}
		
		public int getInStack() {
			return in_stack;
		}
		public void setInStack(int in_stack) {
			this.in_stack = in_stack;
		}
		
		public void addOneToStack() {
			in_stack++;
		}
		public void removeOneFromStack() {
			in_stack--;
			
			if(in_stack <= 0 && parent != null) {
				parent.update();
			}
		}
	}
	
	private Array<ItemsStack> items;
	//private int slots_allocated;
	private int slots_amount;
	
	public ItemsContainer(int slots_amount) {
		this.slots_amount = slots_amount;
		items = new Array<ItemsStack>(slots_amount);
		items.setSize(slots_amount);
	}
	
	//add new 1 item by given type
	public boolean addItem(Item item, int count) {
		boolean out = true;
		for(int i = 0; i < count; i++) {
			if(!addItem(item)) {
				out = false;
				break;
			}
		}
		return out;
	}

	public void clear() {
		items.clear();
		items.setSize(slots_amount);
	}

	/** call f.e when some stack are 0 size so we need to delete it (also called by items stack class) **/
	public void update() {
		for(int i = 0; i < items.size; i++) {
			ItemsStack stack = items.get(i);
			if(stack != null && stack.getInStack() == 0) {
				items.set(i, null);
			}
		}
	}
	
	//returned boolean means if adding item to storage was succesfull
	public boolean addItem(Item item) {
		if(item.isStackable()) {
			//if we found not full stack add new one
			for(int i = 0; i < items.size; i++) {
				ItemsStack stack = items.get(i);
				
				if(stack != null) {
					if(stack.getItem().getItemID() == item.getItemID()) {
						if(stack.getInStack() < item.max_in_stack) {
							stack.addOneToStack();
							return true;
						}
					}
				}
			}

			//if every stack is full try to create new one
			if(slots_amount >= getSlotsAllocated() + 1) {
				for(int i = 0; i < items.size; i++) {
					if(items.get(i) == null) {
						items.set(i, new ItemsStack(item, 1, this));
						return true;
					}
				}

				/*if(index_to_put != -1) {
					items.set(index_to_put, new ItemsStack(item, 1, this));
					slots_allocated++;
					return true;
				} else {
					items.add(new ItemsStack(item, 1, this));
					slots_allocated++;
					return true;
				}*/
			}
			return false;
		} else {
			//if item is not stackable just add new item instance to inventory if there is enough space
			if(slots_amount >= getSlotsAllocated() + 1) {
				for(int i = 0; i < items.size; i++) {
					if(items.get(i) == null) {
						items.set(i, new ItemsStack(item, 1, this));
						return true;
					}
				}
			}

			return false;
		}
	}
	
	//remove n items og type item
	public boolean removeItem(Item item, int count) {
		for(int i = 0; i < count; i++) {
			if(!removeItem(item)) {
				return false;
			}
		}
		return true;
	}
	
	//remove one item
	public boolean removeItem(Item item) {
		for(int i = 0; i < items.size; i++) {
			ItemsStack stack = items.get(i);
			if(stack != null) {
				if(stack.getItem().getItemID() == item.getItemID()) {
					if(stack.getItem().isStackable()) {
						stack.setInStack(stack.getInStack() - 1);
						if(stack.getInStack() <= 0) {
							items.set(i, null);
						}
						return true;
					} else {
						items.set(i, null);
						return true;
					}
				}
			}
		}
		return false;
	}

	public int getSlotsAllocated() {
		int slots_allocated = 0;

		for(int i = 0; i < slots_amount; i++)
			if(items.get(i) != null)
				slots_allocated++;

		return slots_allocated;
	}

	public int getSlotsAmount() {
		return slots_amount;
	}
	public void setSlotsAmount(int slots_amount) {
		this.slots_amount = slots_amount;
	}

	//public void setSlotsAllocated(int slots_allocated) {
	//	this.slots_allocated = slots_allocated;
	//}

	public Array<ItemsStack> getItems() {
		return items;
	}
}
