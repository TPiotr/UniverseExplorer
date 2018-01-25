package explorer.world.inventory;

import com.badlogic.gdx.utils.Array;

/**
 * Class that handles adding/ removing items to some container called ItemsContainer
 * Usefull for making some inventory etc.
 */
public class ItemsContainer {

	/**
	 * ItemsStack class responsible to holding items in stacks
	 */
	public static class ItemsStack {

		/**
		 * Item instance which this stack is holding
		 */
		private Item item;

		/**
		 * Informs how much items in this stack is
		 */
		private int in_stack;

		/**
		 * Parent container, tells in which ItemsContainer this stack is stored
		 */
		private ItemsContainer parent;

		/**
		 * Basic constructor of ItemsStack class
		 * @param item item instance which will be stored in this stack
		 * @param in_stack how much items in this stack is
		 * @param parent ItemsContainer instance where this ItemsStack stored is
		 */
		public ItemsStack(Item item, int in_stack, ItemsContainer parent) {
			this.parent = parent;
			this.item = item;
			this.in_stack = in_stack;
		}

		/**
		 * Clone constructor
		 * @param to_clone instance which will be cloned
		 */
		public ItemsStack(ItemsStack to_clone) {
			this.parent = to_clone.parent;
			this.item = to_clone.item;
			this.in_stack = to_clone.in_stack;
		}

		/**
		 * Clone informations from other ItemsStack instance
		 * @param other
		 */
		public void set(ItemsStack other) {
			this.parent = other.parent;
			this.item = other.item;
			this.in_stack = other.in_stack;
		}

		/**
		 * Getter for parent container
		 * @return get parent ItemsContainer where this stack is stored
		 */
		public ItemsContainer getParent() {
			return parent;
		}

		/**
		 * Setter for parent container
		 * @param parent set new parent of this ItemsStack
		 */
		public void setParent(ItemsContainer parent) {
			this.parent = parent;
		}

		/**
		 * Getter for item stored in this stack
		 * @return items which is stored in this stack
		 */
		public Item getItem() {
			return item;
		}

		/**
		 * Getter for how many items are in stack
		 * @return how much items are in this stack
		 */
		public int getInStack() {
			return in_stack;
		}

		/**
		 * Setter for how many items are in stack
		 * @param in_stack set how many are there
		 */
		public void setInStack(int in_stack) {
			this.in_stack = in_stack;
		}

		/**
		 * Add one item to stack
		 */
		public void addOneToStack() {
			in_stack++;
		}

		/**
		 * Remove one item from stack, and check if stack is not empty if so calls update() method in parent ItemsContainer
		 */
		public void removeOneFromStack() {
			in_stack--;
			
			if(in_stack <= 0 && parent != null) {
				parent.update();
			}
		}
	}

	/**
	 * Array containing all items in this container
	 */
	private Array<ItemsStack> items;

	/**
	 * Amount of slots in this container
	 */
	private int slots_amount;

	/**
	 * Constructs new ItemsContainer instance
	 * @param slots_amount slots amount of new container
	 */
	public ItemsContainer(int slots_amount) {
		this.slots_amount = slots_amount;
		items = new Array<ItemsStack>(slots_amount);
		items.setSize(slots_amount);
	}

	/**
	 * Add multiple items to this container
	 * @param item item type instance
	 * @param count count of new items
	 * @return true if all items were added, false otherwise
	 */
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

	/**
	 * Clear this container from all items
	 */
	public void clear() {
		items.clear();
		items.setSize(slots_amount);
	}

	/**
	 * Call this to force container to check all items if some of the stacks are <=0 size so they had to be removed
	 * Called in ItemsStack in removeOneFromStack method
	 */
	public void update() {
		for(int i = 0; i < items.size; i++) {
			ItemsStack stack = items.get(i);
			if(stack != null && stack.getInStack() == 0) {
				items.set(i, null);
			}
		}
	}

	/**
	 * Add one item to container, new stack will be created or if item is stackable system will try to add one to some existing stack
	 * @param item item type instance
	 * @return true if object was added, false otherwise
	 */
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

	/**
	 * Remove multiple items from this container
	 * @param item item instance type (don't have to be same instance just ID of item must be same)
	 * @param count how many items have to be removed
	 * @return true if all items were removed, false otherwise
	 */
	public boolean removeItem(Item item, int count) {
		for(int i = 0; i < count; i++) {
			if(!removeItem(item)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Remove one item from this container
	 * @param item tem instance type (don't have to be same instance just ID of item must be same)
	 * @return true if item was removed, false otherwise
	 */
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

	/**
	 * Get amount of allocated slots
	 * @return amount of allocated slots
	 */
	public int getSlotsAllocated() {
		int slots_allocated = 0;

		for(int i = 0; i < slots_amount; i++)
			if(items.get(i) != null)
				slots_allocated++;

		return slots_allocated;
	}

	/**
	 * Getter for slots amount
	 * @return slots amount
	 */
	public int getSlotsAmount() {
		return slots_amount;
	}

	/**
	 * Setter for slots amount
	 * @param slots_amount new slots amount
	 */
	public void setSlotsAmount(int slots_amount) {
		this.slots_amount = slots_amount;
	}

	/**
	 * Getter for array that contains all items stacks
	 * @return array that contains all items stacks
	 */
	public Array<ItemsStack> getItems() {
		return items;
	}
}
