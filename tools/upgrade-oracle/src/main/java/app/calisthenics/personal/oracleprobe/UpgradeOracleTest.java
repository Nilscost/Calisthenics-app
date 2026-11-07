package app.calisthenics.personal.oracleprobe;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;

/** Runs against installed target classes, never bundles app/domain implementations. */
@RunWith(AndroidJUnit4.class)
public class UpgradeOracleTest {
    private Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private SharedPreferences pref(String name) { return context().getSharedPreferences(name, Context.MODE_PRIVATE); }
    private Object call(String store, String method, Class<?>[] types, Object... args) throws Exception {
        Class<?> c = context().getClassLoader().loadClass("io.github.gonbei774.calisthenicsmemory.ui.screens." + store);
        return c.getMethod(method, types).invoke(c.getField("INSTANCE").get(null), args);
    }
    private Object load(String store) throws Exception { return call(store, "load", new Class<?>[]{Context.class}, context()); }
    private Object getter(Object model, String method) throws Exception { return model.getClass().getMethod(method).invoke(model); }
    private void commit(SharedPreferences.Editor e) { assertTrue("fixture write", e.commit()); }

    @Test public void setup() throws Exception {
        assertEquals("app.calisthenics.personal", context().getPackageName());
        // SeedReceiver ran first; preserve its valid original routine structure while giving it a distinctive identity.
        JSONObject routine = new JSONObject(pref("routine").getString("r", ""));
        routine.put("id", "oracle-routine"); routine.put("name", "Oracle routine"); routine.put("revision", 7);
        commit(pref("routine").edit().putString("r", routine.toString()));
        JSONObject saved = new JSONObject().put("id", "oracle-saved").put("name", "Oracle saved").put("revision", 4)
            .put("routine", routine).put("sets", 3).put("warmupOn", true).put("restDurations", new JSONObject().put("slot-a", 73));
        commit(pref("saved_routines").edit().putString("list", new JSONArray().put(saved).toString()));
        JSONObject prefs = new JSONObject(pref("planner_prefs").getString("prefs", "{}"));
        prefs.put("appVolumePercent", 47).put("audioEnabled", false).put("autoProgression", false).put("selectedProfileId", "home");
        commit(pref("planner_prefs").edit().putString("prefs", prefs.toString()));
        commit(pref("personal_targets").edit().putString("values", "{\"pushup-standard\":{\"revision\":3,\"values\":[4,6,8,10,12],\"modifiedAtEpochMs\":123456}}"));
        commit(pref("levels").edit().putString("v", "{\"pushup-standard\":3,\"squat-air\":2}")
            .putString("days", "{\"pushup-standard\":19000,\"squat-air\":19001}").putLong("resetAtMs", 123456L));
        commit(pref("goal").edit().putString("id", "muscle-up"));
        commit(pref("mode").edit().putBoolean("timed", true));
        commit(pref("onboarding").edit().putBoolean("done", true));
        commit(pref("theme_preferences").edit().putString("app_theme", "dark"));
        commit(pref("suggestions").edit().putStringSet("dismissed", new HashSet<>(Arrays.asList("pushup-standard", "squat-air")))
            .putString("accepted", "pushup-standard|3|19000").putString("acceptedNext", "pushup-standard|pushup-diamond|19001"));
        commit(pref("train").edit().putInt("rounds", 3).putString("format", "PAIRS").putBoolean("warmup", true)
            .putInt("preparation_seconds", 9).putStringSet("additional_pools", new HashSet<>())
            .putInt("ordinary_rule_default_version", 1).putInt("ordinary_draft_rule_default_version", 1));
        commit(pref("preview_draft").edit().putString("draft", "{\"key\":\"muscle-up|-|-\",\"rest\":{\"slot-a\":73},\"modeRevision\":4,\"ruleDefaultVersion\":1}"));
        verifyValues();
    }

    @Test public void verify() throws Exception { verifyValues(); }

    @Test public void setupCheckpoint() throws Exception {
        String plan;
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(context().getDatabasePath("bodyweight_trainer_database").getPath(), null, SQLiteDatabase.OPEN_READONLY);
             Cursor cursor = db.rawQuery("SELECT planJson FROM plan_snapshots ORDER BY planId LIMIT 1", null)) {
            assertTrue("seed plan exists", cursor.moveToFirst()); plan = cursor.getString(0);
        }
        JSONObject checkpoint = new JSONObject().put("sessionId", "oracle-checkpoint").put("plan", new JSONObject(plan))
            .put("phase", "PAUSED").put("blockIndex", 0).put("remainingMs", 17000).put("executions", new JSONObject())
            .put("activeMs", new JSONObject()).put("activeElapsedMs", 3000).put("eventSequence", 4)
            .put("replacements", new JSONObject()).put("appliedCueIds", new JSONArray()).put("pauseReason", "USER")
            .put("startedAtEpochMs", 1000).put("savedAtEpochMs", 2000).put("logged", new JSONObject());
        commit(pref("workout_checkpoint").edit().putString("json", checkpoint.toString()));
        verifyCheckpoint();
    }

    @Test public void verifyCheckpoint() throws Exception {
        Class<?> codec = context().getClassLoader().loadClass("io.github.gonbei774.calisthenicsmemory.session.CheckpointCodec");
        Object checkpoint = codec.getMethod("decode", String.class).invoke(codec.getField("INSTANCE").get(null), pref("workout_checkpoint").getString("json", null));
        assertNotNull("checkpoint decodes", checkpoint); assertEquals("oracle-checkpoint", getter(checkpoint, "getSessionId"));
        assertEquals("PAUSED", getter(checkpoint, "getPhase").toString()); assertEquals(17000L, getter(checkpoint, "getRemainingMs"));
        assertEquals(4L, getter(checkpoint, "getEventSequence")); assertEquals("USER", getter(checkpoint, "getPauseReason").toString());
    }

    @Test public void setupLegacy() throws Exception {
        JSONObject routine = new JSONObject(pref("routine").getString("r", ""));
        routine.put("rule", new JSONObject().put("kind", "REP_RANGE"));
        commit(pref("routine").edit().putString("r", routine.toString()));
        commit(pref("goal").edit().putString("id", "general"));
        commit(pref("profiles").edit().clear());
        commit(pref("mode").edit().putBoolean("highBar", true));
        commit(pref("train").edit().clear().putStringSet("additional_pools", new HashSet<>(Arrays.asList("power"))));
        commit(pref("preview_draft").edit().putString("draft", "{\"key\":\"general|-|-\",\"edits\":{\"rule\":{\"kind\":\"REP_RANGE\"}},\"rest\":{\"slot-a\":73},\"ruleDefaultVersion\":0}"));
        verifyLegacy();
        // Wait for the old app's asynchronous migration writes before instrumentation exits.
        for (String file : Arrays.asList("mode", "train", "routine", "preview_draft", "profiles")) commit(pref(file).edit());
    }

    @Test public void verifyLegacy() throws Exception {
        List<?> profiles = (List<?>)load("ProfileStore");
        Object home = null;
        for (Object profile : profiles) if ("home".equals(getter(profile, "getId"))) home = profile;
        assertNotNull("home profile", home);
        boolean hasHighBar = false;
        for (Object item : (List<?>)getter(home, "getItems")) if ("high-bar".equals(getter(item, "getEquipmentId"))) hasHighBar = true;
        assertTrue("legacy high-bar migrated", hasHighBar); assertFalse(pref("mode").contains("highBar"));
        Class<?> train = context().getClassLoader().loadClass("io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore");
        train.getMethod("load", Context.class).invoke(train.getField("INSTANCE").get(null), context());
        assertEquals(new HashSet<>(), pref("train").getStringSet("additional_pools", null));
        assertEquals(1, pref("train").getInt("ordinary_rule_default_version", 0));
        assertEquals(1, pref("train").getInt("ordinary_draft_rule_default_version", 0));
        Object routine = load("RoutineStore"); assertEquals("APP_LEVELS", getter(getter(routine, "getRule"), "getKind").toString());
        Class<?> draftStore = context().getClassLoader().loadClass("io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore");
        Object draft = draftStore.getMethod("get", Context.class, String.class).invoke(draftStore.getField("INSTANCE").get(null), context(), "general|-|-");
        assertNotNull(draft); assertEquals(1, getter(draft, "getRuleDefaultVersion"));
        assertEquals("APP_LEVELS", getter(getter(getter(draft, "getEdits"), "getRule"), "getKind").toString());
        assertEquals(73, ((Map<?,?>)getter(draft, "getRest")).get("slot-a"));
    }

    @SuppressWarnings("unchecked") private void verifyValues() throws Exception {
        assertEquals("muscle-up", load("GoalStore"));
        assertEquals(true, call("ModeStore", "timed", new Class<?>[]{Context.class}, context()));
        assertEquals(true, call("OnboardingStore", "done", new Class<?>[]{Context.class}, context()));
        assertTrue(pref("onboarding").getBoolean("done", false));
        Object preferences = load("PrefsStore");
        assertEquals(47, getter(preferences, "getAppVolumePercent")); assertEquals(false, getter(preferences, "getAudioEnabled"));
        assertEquals(false, getter(preferences, "getAutoProgression")); assertEquals("home", getter(preferences, "getSelectedProfileId"));
        Object routine = load("RoutineStore"); assertEquals("oracle-routine", getter(routine, "getId"));
        assertEquals("Oracle routine", getter(routine, "getName")); assertEquals(7, getter(routine, "getRevision"));
        List<?> saved = (List<?>)load("SavedRoutineStore"); assertEquals(1, saved.size());
        assertEquals("oracle-saved", getter(saved.get(0), "getId")); assertEquals(4, getter(saved.get(0), "getRevision"));
        assertEquals(3, getter(saved.get(0), "getSets")); assertEquals(true, getter(saved.get(0), "getWarmupOn"));
        assertEquals(73, ((Map<?,?>)getter(saved.get(0), "getRestDurations")).get("slot-a"));
        Map<?,?> targets = (Map<?,?>)load("PersonalTargetStore"); Object target = targets.get("pushup-standard"); assertNotNull(target);
        assertEquals(3, getter(target, "getRevision")); assertEquals(Arrays.asList(4,6,8,10,12), getter(target, "getValues"));
        assertEquals(123456L, getter(target, "getModifiedAtEpochMs"));
        assertEquals(3, ((Map<?,?>)load("LevelStore")).get("pushup-standard"));
        assertEquals(19000, ((Map<?,?>)call("LevelStore", "days", new Class<?>[]{Context.class}, context())).get("pushup-standard"));
        assertEquals(123456L, call("LevelStore", "resetAtMs", new Class<?>[]{Context.class}, context()));
        List<?> profiles = (List<?>)load("ProfileStore"); assertEquals(2, profiles.size());
        assertEquals("home", getter(profiles.get(0), "getId")); assertEquals("travel", getter(profiles.get(1), "getId"));
        boolean anchoredBand = false, stableChair = false;
        for (Object item : (List<?>)getter(profiles.get(0), "getItems"))
            if ("resistance-band".equals(getter(item, "getEquipmentId"))) anchoredBand = ((java.util.Set<?>)getter(item, "getSuitability")).contains("stable-anchor");
        for (Object item : (List<?>)getter(profiles.get(1), "getItems"))
            if ("chair".equals(getter(item, "getEquipmentId"))) stableChair = ((java.util.Set<?>)getter(item, "getSuitability")).contains("stable");
        assertTrue("anchored band", anchoredBand); assertTrue("stable chair", stableChair);
        assertEquals(new HashSet<>(Arrays.asList("pushup-standard", "squat-air")), call("SuggestionStore", "dismissed", new Class<?>[]{Context.class}, context()));
        List<?> actions = (List<?>)call("SuggestionStore", "accepted", new Class<?>[]{Context.class}, context());
        assertEquals(2, actions.size()); assertEquals("pushup-standard", getter(actions.get(0), "getVariationId"));
        assertEquals(3, getter(actions.get(0), "getTier")); assertEquals(19000, getter(actions.get(0), "getDay"));
        assertEquals("pushup-diamond", getter(actions.get(1), "getToVariationId")); assertEquals(19001, getter(actions.get(1), "getDay"));
        assertTrue(pref("suggestions").getAll().get("dismissed") instanceof java.util.Set);
        assertTrue(pref("train").getAll().get("additional_pools") instanceof java.util.Set);
        assertEquals(3, pref("train").getInt("rounds", 0)); assertEquals("PAIRS", pref("train").getString("format", null));
        assertEquals(9, pref("train").getInt("preparation_seconds", 0));
        assertEquals("dark", pref("theme_preferences").getString("app_theme", null));
        Class<?> themeClass = context().getClassLoader().loadClass("io.github.gonbei774.calisthenicsmemory.data.ThemePreferences");
        Object theme = themeClass.getConstructor(Context.class).newInstance(context());
        assertEquals("DARK", themeClass.getMethod("getTheme").invoke(theme).toString());
        Class<?> trainClass = context().getClassLoader().loadClass("io.github.gonbei774.calisthenicsmemory.ui.train.TrainSettingsStore");
        Object settings = trainClass.getMethod("load", Context.class).invoke(trainClass.getField("INSTANCE").get(null), context());
        assertEquals(3, getter(settings, "getRounds")); assertEquals("PAIRS", getter(settings, "getFormat").toString());
        assertEquals(9, getter(settings, "getPreparationSeconds"));
        Class<?> draftStore = context().getClassLoader().loadClass("io.github.gonbei774.calisthenicsmemory.ui.train.PreviewDraftStore");
        Object draft = draftStore.getMethod("get", Context.class, String.class).invoke(draftStore.getField("INSTANCE").get(null), context(), "muscle-up|-|-");
        assertNotNull("draft decoded", draft); assertEquals(4L, getter(draft, "getModeRevision"));
        assertEquals(1, getter(draft, "getRuleDefaultVersion")); assertEquals(73, ((Map<?,?>)getter(draft, "getRest")).get("slot-a"));
    }
}
