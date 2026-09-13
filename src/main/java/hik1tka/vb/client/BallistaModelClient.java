package hik1tka.vb.client;

import hik1tka.vb.client.render.BallistaDynamicBakedModel;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.util.ModelIdentifier;
import net.minecraft.util.Identifier;

import javax.naming.Context;

/**
 * Хук у процес спікання (bake) моделей на клієнті. Знаходить модель саме
 * нашого блока балісти і підміняє звичайну BakedModel на
 * {@link BallistaDynamicBakedModel}, яка вже сама вирішує, яку текстуру
 * малювати, залежно від матеріалу конкретного блока.
 * <p>
 * ВАЖЛИВО: цей клас треба зареєструвати у клієнтському entrypoint-і мода,
 * інакше нічого не станеться. Див. інструкцію нижче в чаті.
 */
public class BallistaModelClient implements ModelLoadingPlugin {

    private static final String NAMESPACE = "villagebounds";
    private static final String PATH = "ballista";

    @Override
    public void onInitializeModelLoader(Context pluginContext) {
        pluginContext.modifyModelAfterBake().register((model, context) -> {
            if (isBallistaModel(context.id())) {
                return new BallistaDynamicBakedModel(model);
            }
            return model;
        });
    }

    private boolean isBallistaModel(Identifier id) {
        // Модель блока має id "villagebounds:ballista" (варіант ""),
        // модель предмета в інвентарі - "villagebounds:ballista" (варіант "inventory").
        // В обох випадках namespace/path однакові - саме їх і звіряємо.
        if (id instanceof ModelIdentifier modelId) {
            return NAMESPACE.equals(modelId.getNamespace()) && PATH.equals(modelId.getPath());
        }
        return false;
    }
}
