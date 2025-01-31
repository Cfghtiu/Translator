package kgg.translator.event;

import kgg.translator.translator.Translator;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface SetTranslatorEvent {
    Event<SetTranslatorEvent> EVENT = EventFactory.createArrayBacked(SetTranslatorEvent.class,
        (listeners) -> (translator) -> {
            for (SetTranslatorEvent listener : listeners) {
                listener.setTranslator(translator);
            }
        });
    void setTranslator(Translator translator);
    
    static void invoke(Translator translator) {
        EVENT.invoker().setTranslator(translator);
    }
}
