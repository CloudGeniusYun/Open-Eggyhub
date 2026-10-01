package com.eggyhub.android;

import androidx.annotation.Keep;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Keep
@JsonAdapter(ArticleGroup.Deserializer.class)
public class ArticleGroup {
    @SerializedName("category")
    private String category;

    @SerializedName("items")
    private List<ArticleItem> items;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<ArticleItem> getItems() {
        return items;
    }

    public void setItems(List<ArticleItem> items) {
        this.items = items;
    }

    public static class Deserializer implements JsonDeserializer<ArticleGroup> {
        @Override
        public ArticleGroup deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            JsonObject jsonObject = json.getAsJsonObject();
            ArticleGroup group = new ArticleGroup();
            
            if (jsonObject.has("category")) {
                group.setCategory(jsonObject.get("category").getAsString());
            }
            
            if (jsonObject.has("items")) {
                JsonArray itemsArray = jsonObject.getAsJsonArray("items");
                List<ArticleItem> items = new ArrayList<>();
                for (JsonElement itemElement : itemsArray) {
                    JsonObject itemObj = itemElement.getAsJsonObject();
                    ArticleItem item = new ArticleItem(
                        itemObj.has("title") ? itemObj.get("title").getAsString() : "",
                        itemObj.has("author") ? itemObj.get("author").getAsString() : "",
                        group.getCategory(),
                        itemObj.has("id") ? itemObj.get("id").getAsInt() : 0,
                        itemObj.has("date") ? itemObj.get("date").getAsString() : ""
                    );
                    if (itemObj.has("content") && !itemObj.get("content").isJsonNull()) {
                        item.setContent(itemObj.get("content").getAsString());
                    }
                    items.add(item);
                }
                group.setItems(items);
            }
            
            return group;
        }
    }
}
