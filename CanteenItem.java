public class CanteenItem {
    String itemId, itemName, cost, quantity, rating;

    public CanteenItem(String itemId, String itemName, String cost, String quantity, String rating) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.cost = cost;
        this.quantity = quantity;
        this.rating = rating;
    }

    @Override
    public String toString() {
        return itemId + "\t" + itemName + "\t" + cost + "\t" + quantity + "\t" + rating;
    }
}
