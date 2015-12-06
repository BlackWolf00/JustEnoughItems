package mezz.jei.gui;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;

import mezz.jei.api.JEIManager;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeHandler;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.util.Log;
import mezz.jei.util.MathUtil;

public class RecipeGuiLogic implements IRecipeGuiLogic {
	/* Whether this GUI is dispzzlaying input or output recipes */
	private Mode mode;

	/* The focus of this GUI */
	@Nonnull
	private Focus focus = new Focus();

	/* List of Recipe Categories that involve the focus */
	@Nonnull
	private ImmutableList<IRecipeCategory> recipeCategories = ImmutableList.of();

	/* List of recipes for the currently selected recipeClass */
	@Nonnull
	private List<Object> recipes = Collections.emptyList();

	private int recipesPerPage = 0;
	private int recipeCategoryIndex = 0;
	private int pageIndex = 0;

	@Override
	public boolean setFocus(@Nonnull Focus focus, @Nonnull Mode mode) {
		if (this.focus.equals(focus) && this.mode == mode) {
			return true;
		}

		ImmutableList<IRecipeCategory> types = null;
		switch (mode) {
			case INPUT:
				types = focus.getCategoriesWithInput();
				break;
			case OUTPUT:
				types = focus.getCategoriesWithOutput();
				break;
		}
		if (types.isEmpty()) {
			return false;
		}

		this.recipeCategories = types;
		this.focus = focus;
		this.mode = mode;

		this.recipeCategoryIndex = 0;
		this.pageIndex = 0;

		Container container = Minecraft.getMinecraft().thePlayer.openContainer;
		if (container != null) {
			for (int i = 0; i < recipeCategories.size(); i++) {
				IRecipeCategory recipeCategory = recipeCategories.get(i);
				if (JEIManager.recipeRegistry.getRecipeTransferHelper(container, recipeCategory) != null) {
					this.recipeCategoryIndex = i;
					break;
				}
			}
		}

		updateRecipes();

		return true;
	}

	@Override
	public boolean setCategoryFocus() {
		IRecipeCategory recipeCategory = getRecipeCategory();
		if (recipeCategory == null) {
			return false;
		}

		if (this.focus.isBlank()) {
			return false;
		}

		this.recipeCategories = JEIManager.recipeRegistry.getRecipeCategories();
		this.focus = new Focus();

		this.recipeCategoryIndex = this.recipeCategories.indexOf(recipeCategory);
		this.pageIndex = 0;

		updateRecipes();

		return true;
	}

	@Override
	public void setRecipesPerPage(int recipesPerPage) {
		if (this.recipesPerPage != recipesPerPage) {
			int recipeIndex = pageIndex * this.recipesPerPage;
			this.pageIndex = recipeIndex / recipesPerPage;

			this.recipesPerPage = recipesPerPage;
			updateRecipes();
		}
	}
	
	private void updateRecipes() {
		IRecipeCategory recipeCategory = getRecipeCategory();
		if (focus.isBlank()) {
			recipes = JEIManager.recipeRegistry.getRecipes(recipeCategory);
		} else {
			switch (mode) {
				case INPUT:
					recipes = focus.getRecipesWithInput(recipeCategory);
					break;
				case OUTPUT:
					recipes = focus.getRecipesWithOutput(recipeCategory);
					break;
			}
		}
	}

	@Override
	public IRecipeCategory getRecipeCategory() {
		if (recipeCategories.size() == 0) {
			return null;
		}
		return recipeCategories.get(recipeCategoryIndex);
	}

	@Override
	@Nonnull
	public List<RecipeLayout> getRecipeWidgets(int posX, int posY, int spacingY) {
		List<RecipeLayout> recipeWidgets = new ArrayList<>();

		IRecipeCategory recipeCategory = getRecipeCategory();
		if (recipeCategory == null) {
			return recipeWidgets;
		}

		int recipeWidgetIndex = 0;
		for (int recipeIndex = pageIndex * recipesPerPage; recipeIndex < recipes.size() && recipeWidgets.size() < recipesPerPage; recipeIndex++) {
			Object recipe = recipes.get(recipeIndex);
			IRecipeHandler recipeHandler = JEIManager.recipeRegistry.getRecipeHandler(recipe.getClass());
			if (recipeHandler == null) {
				Log.error("Couldn't find recipe handler for recipe: {}", recipe);
				continue;
			}

			@SuppressWarnings("unchecked")
			IRecipeWrapper recipeWrapper = recipeHandler.getRecipeWrapper(recipe);

			RecipeLayout recipeWidget = new RecipeLayout(recipeWidgetIndex++, posX, posY, recipeCategory, recipeWrapper, focus);
			recipeWidgets.add(recipeWidget);

			posY += spacingY;
		}

		return recipeWidgets;
	}

	@Override
	public void nextRecipeCategory() {
		int recipesTypesCount = recipeCategories.size();
		recipeCategoryIndex = (recipeCategoryIndex + 1) % recipesTypesCount;
		pageIndex = 0;
		updateRecipes();
	}

	@Override
	public boolean hasMultiplePages() {
		return recipes.size() > recipesPerPage;
	}

	@Override
	public void previousRecipeCategory() {
		int recipesTypesCount = recipeCategories.size();
		recipeCategoryIndex = (recipesTypesCount + recipeCategoryIndex - 1) % recipesTypesCount;
		pageIndex = 0;
		updateRecipes();
	}

	@Override
	public void nextPage() {
		int pageCount = pageCount(recipesPerPage);
		pageIndex = (pageIndex + 1) % pageCount;
		updateRecipes();
	}

	@Override
	public void previousPage() {
		int pageCount = pageCount(recipesPerPage);
		pageIndex = (pageCount + pageIndex - 1) % pageCount;
		updateRecipes();
	}

	private int pageCount(int recipesPerPage) {
		if (recipes.size() <= 1) {
			return 1;
		}

		return MathUtil.divideCeil(recipes.size(), recipesPerPage);
	}

	@Override
	@Nonnull
	public String getPageString() {
		return (pageIndex + 1) + "/" + pageCount(recipesPerPage);
	}

	@Override
	public boolean hasMultipleCategories() {
		return recipeCategories.size() > 1;
	}
}
