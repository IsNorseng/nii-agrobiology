package ru.agrobiology.nii;

import java.util.Arrays;

public final class Game {
    public static final String[] NAMES = {"Баклажан", "Огурец", "Кабачок", "Морковь", "Банан"};

    private static final double[] INITIAL_LENGTH = {10.6, 9.9, 10.0, 10.8, 10.4};
    private static final double[] INITIAL_WIDTH = {3.6, 2.8, 4.0, 2.6, 3.0};
    private static final double[] GROWTH = {0.87, 0.94, 0.88, 0.80, 0.88};
    private static final int[] GOALS = {30, 40, 50, 75, 100};
    private static final String[] STATE_TAGS = {
        "polished", "irritated", "flies", "film", "overfed", "warped", "wet", "dry", "fuzz",
        "dirty", "hungry", "thick", "thin", "long", "shaved", "clean"
    };
    private static final int LINE_HISTORY = 24;
    private static final String INTRO = "Опытный образец передан на ответственное хранение. Инструкция признана избыточной.";
    private static final String[] STAGE_CONCLUSIONS = {
        "Образец признан овощем.\nОснований для восторга не обнаружено.",
        "Длина увеличилась.\nПросим воздержаться от преждевременных выводов о длине.",
        "Образец занимает полметра.\nОсвободившееся место не обнаружено.",
        "Измерение произведено в два приёма.\nВторой приём засекречен по просьбе линейки.",
        "Метр подтверждён.\nДля дальнейшего измерения требуется коридор."
    };
    private static final String[] LATER_CONCLUSIONS = {
        "Образец временно считается участком местности.",
        "Измерительная комиссия растянулась вдоль объекта. Потерь нет.",
        "Повторное измерение подтвердило отсутствие конца в пределах видимости."
    };

    public final int vegetable;
    public double length, thickness;
    public int moisture = 51, nutrition = 47, cleanliness = 72, fuzz = 12, irritation;
    public int polish, warp, turn, stage, lastAction = -1;

    private int streak, lastShave = -10, nextLine = 2, line = -1, historyCursor;
    private int newStates;
    private double lastGrowth;
    private int random;
    private final int[] history = new int[LINE_HISTORY];

    public Game(int vegetable) {
        if (vegetable < 0 || vegetable >= NAMES.length) {
            throw new IllegalArgumentException("Unknown vegetable");
        }
        this.vegetable = vegetable;
        length = INITIAL_LENGTH[vegetable];
        thickness = INITIAL_WIDTH[vegetable];
        warp = vegetable == 4 ? 12 : 0;
        random = (int) System.nanoTime() ^ (vegetable + 1) * 0x5A17A7EB;
        if (random == 0) random = 1;
        Arrays.fill(history, -1);
    }

    public int goal() {
        return stage < GOALS.length ? GOALS[stage] : 100 + (stage - GOALS.length + 1) * 25;
    }

    public boolean pendingReport() {
        return length >= goal();
    }

    public void continueExperiment() {
        if (!pendingReport()) return;
        stage++;
        newStates = 0;
        chooseDialogue(true);
    }

    public void act(int action) {
        if (pendingReport() || action < 0 || action > 3) return;
        int previousStates = activeStates();
        streak = action == lastAction ? streak + 1 : 1;
        lastAction = action;
        turn++;
        switch (action) {
            case 0:
                moisture = bounded(moisture + 20);
                cleanliness += 10;
                polish -= 4;
                break;
            case 1:
                nutrition = bounded(nutrition + 25);
                cleanliness -= nutrition > 80 ? 10 : 4;
                polish -= 2;
                double widthGain = nutrition > 81 ? 0.19 + (nutrition - 81) * 0.006 : 0.065;
                thickness += widthGain * (vegetable == 2 ? 1.72 : 1.0);
                break;
            case 2:
                cleanliness += 20;
                irritation += 4 + (streak > 1 ? 7 : 0) + (polish > 50 ? 4 : 0);
                polish += vegetable == 0 ? 24 : 18;
                break;
            case 3:
                irritation += (fuzz < 13 ? 10 : 5) + (streak > 1 ? 7 : 0);
                fuzz -= 35;
                cleanliness += 5;
                lastShave = turn;
                break;
            default:
                return;
        }

        moisture = bounded(moisture - (moisture > 78 ? 6 : 3));
        nutrition = bounded(nutrition - (nutrition > 78 ? 6 : 2));
        cleanliness = bounded(cleanliness - 3 - (nutrition >= 78 ? 3 : 0) - (moisture >= 83 ? 3 : 0));
        polish = bounded(polish - (action == 2 ? 2 : 5));
        fuzz = bounded(fuzz + (vegetable == 1 ? 4 : 2) + (nutrition >= 78 ? 1 : 0));
        irritation -= action < 2 ? 6 : 2;
        if (moisture >= 90 && nutrition >= 82) irritation++;
        irritation = bounded(irritation);

        int deformation = (nutrition >= 85 ? 4 : 0) + (irritation >= 59 ? 4 : 0);
        if (vegetable == 4) {
            deformation += 1 + (moisture >= 81 || nutrition >= 78 ? 3 : 0);
        } else if (deformation == 0) {
            deformation = -1;
        }
        warp = bounded(warp + deformation);

        double quality = 0.49 + 0.51 * (0.55 * balance(moisture) + 0.45 * balance(nutrition));
        double stress = 1.0 - 0.26 * irritation / 100.0;
        if (moisture >= 83) stress *= 0.87;
        if (nutrition >= 83) stress *= 0.92;
        if (cleanliness < 28) stress *= 0.91;
        double growth = GROWTH[vegetable];
        if (action == 0 && moisture <= 71) growth *= 1.12;
        if (action == 1 && nutrition <= 74) growth *= vegetable == 3 ? 1.45 : 1.17;
        lastGrowth = growth * quality * stress;
        length += lastGrowth;
        double naturalWidth = nutrition >= 38 ? 0.022 : 0.009;
        thickness += naturalWidth * (vegetable == 2 ? 1.35 : 1.0);
        if (nutrition >= 85) thickness += vegetable == 2 ? 0.14 : 0.075;

        newStates |= activeStates() & ~previousStates;
        if (turn >= nextLine && !pendingReport()) chooseDialogue(false);
    }

    public boolean has(String tag) {
        switch (tag) {
            case "strong": return lastGrowth >= 0.88;
            case "dry": return moisture <= 29;
            case "wet": return moisture >= 77;
            case "hungry": return nutrition <= 27;
            case "overfed": return nutrition >= 78;
            case "dirty": return cleanliness <= 46;
            case "clean": return cleanliness >= 82;
            case "shiny": return polish >= 14 && cleanliness >= 60;
            case "polished": return polish >= 43;
            case "fuzz": return fuzz >= 33;
            case "shaved": return turn - lastShave <= 1 && fuzz < 24;
            case "irritated": return irritation >= 32;
            case "flies": return cleanliness <= 36 && nutrition >= 65;
            case "film": return moisture >= 77 && cleanliness <= 52;
            case "warped": return warp >= 36;
            case "thick": return thickness >= 6.6 && (thickness / length >= 0.27 || thickness >= 12.0);
            case "thin": return length >= 17.0 && thickness * 7 < length;
            case "long": return length >= 42.0;
            case "near": return goal() - length <= 4.5;
            case "normal": return moisture >= 40 && moisture <= 71 && nutrition >= 40 && nutrition <= 71
                && cleanliness >= 55 && irritation < 32;
            default: return false;
        }
    }

    public String lineText() {
        return line < 0 ? INTRO : Lines.ALL[line].text;
    }

    public boolean commissionSpeaking() {
        return line < 0 || Lines.ALL[line].commission;
    }

    public String[] report() {
        String surface = "удовлетворительная";
        if (has("polished")) surface = "отражает комиссию";
        else if (has("irritated")) surface = "возражает";
        else if (has("film")) surface = "под дополнительным слоем";
        else if (has("dirty")) surface = "частично установлена";
        else if (has("wet")) surface = "скользкая по форме № 2";
        else if (has("dry")) surface = "нуждается в осадках";
        else if (has("shiny")) surface = "блестит в пределах допуска";

        String hair = "в пределах допуска";
        if (fuzz >= 67) hair = "требует отдельного измерения";
        else if (has("fuzz")) hair = "вызывает вопросы";
        else if (has("shaved")) hair = "отсутствует по акту";

        String conclusion = stage < STAGE_CONCLUSIONS.length ? STAGE_CONCLUSIONS[stage]
            : LATER_CONCLUSIONS[(stage - STAGE_CONCLUSIONS.length) % LATER_CONCLUSIONS.length];
        if (vegetable == 4 && stage == 0) {
            conclusion = "Длина образца подтверждена.\nВопрос классификации снят с повестки.";
        }
        if (has("thick")) conclusion += "\nПоперечная амбициозность превышает рекомендуемые показатели.";
        else if (has("thin")) conclusion += "\nФорма учтена как продольно целеустремлённая.";
        else if (has("warped")) conclusion += "\nУ изгиба обнаружена собственная точка зрения.";
        return new String[] {surface, hair, conclusion};
    }

    private void chooseDialogue(boolean postReport) {
        int active = activeStates();
        int selected = postReport ? select("post") : -1;
        for (int i = 0; selected < 0 && i < STATE_TAGS.length; i++) {
            if ((newStates & active & (1 << i)) != 0) selected = select(STATE_TAGS[i]);
        }
        if (selected < 0 && has("strong")) selected = select("strong");
        if (selected < 0 && has("near")) selected = select("near");
        if (selected < 0 && roll(5) == 0) selected = select("species");
        if (selected < 0 && roll(3) != 0) {
            for (int i = 0; selected < 0 && i < STATE_TAGS.length; i++) {
                if ((active & (1 << i)) != 0) selected = select(STATE_TAGS[i]);
            }
        }
        if (selected < 0 && has("normal")) selected = select("normal");
        if (selected < 0) selected = select("absurd");
        if (selected < 0) selected = select("species");

        if (selected >= 0) {
            line = selected;
            history[historyCursor] = selected;
            historyCursor = (historyCursor + 1) % history.length;
        }
        nextLine = turn + 2 + roll(3);
        newStates = 0;
    }

    private int select(String tag) {
        int count = 0;
        int picked = -1;
        for (int i = 0; i < Lines.ALL.length; i++) {
            Lines.Line entry = Lines.ALL[i];
            if (entry.vegetable != -1 && entry.vegetable != vegetable) continue;
            if (!tag.equals(entry.tag) || recentlySeen(i)) continue;
            if (roll(++count) == 0) picked = i;
        }
        return picked;
    }

    private boolean recentlySeen(int id) {
        for (int seen : history) if (seen == id) return true;
        return false;
    }

    private int activeStates() {
        int active = 0;
        for (int i = 0; i < STATE_TAGS.length; i++) {
            if (has(STATE_TAGS[i])) active |= 1 << i;
        }
        return active;
    }

    private int roll(int bound) {
        random ^= random << 13;
        random ^= random >>> 17;
        random ^= random << 5;
        return (random & Integer.MAX_VALUE) % bound;
    }

    private static double balance(int value) {
        return Math.max(0.0, 1.0 - Math.max(0, Math.abs(value - 55) - 14) / 42.0);
    }

    private static int bounded(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public String save() {
        StringBuilder data = new StringBuilder("1");
        data.append(',').append(vegetable).append(',').append(length).append(',').append(thickness);
        int[] state = {moisture, nutrition, cleanliness, fuzz, irritation, polish, warp, turn, stage,
            lastAction, streak, lastShave, nextLine, line, historyCursor, newStates, random};
        for (int value : state) data.append(',').append(value);
        data.append(',').append(lastGrowth);
        for (int id : history) data.append(',').append(id);
        return data.toString();
    }

    public static Game restore(String data) {
        if (data == null) return null;
        String[] parts = data.split(",");
        if (parts.length != 22 + LINE_HISTORY || !"1".equals(parts[0])) return null;
        try {
            Game game = new Game(Integer.parseInt(parts[1]));
            game.length = Double.parseDouble(parts[2]);
            game.thickness = Double.parseDouble(parts[3]);
            game.moisture = bounded(Integer.parseInt(parts[4]));
            game.nutrition = bounded(Integer.parseInt(parts[5]));
            game.cleanliness = bounded(Integer.parseInt(parts[6]));
            game.fuzz = bounded(Integer.parseInt(parts[7]));
            game.irritation = bounded(Integer.parseInt(parts[8]));
            game.polish = bounded(Integer.parseInt(parts[9]));
            game.warp = bounded(Integer.parseInt(parts[10]));
            game.turn = Integer.parseInt(parts[11]);
            game.stage = Integer.parseInt(parts[12]);
            game.lastAction = Integer.parseInt(parts[13]);
            game.streak = Integer.parseInt(parts[14]);
            game.lastShave = Integer.parseInt(parts[15]);
            game.nextLine = Integer.parseInt(parts[16]);
            game.line = lineId(parts[17]);
            game.historyCursor = Integer.parseInt(parts[18]);
            game.newStates = Integer.parseInt(parts[19]);
            int savedRandom = Integer.parseInt(parts[20]);
            if (savedRandom != 0) game.random = savedRandom;
            game.lastGrowth = Double.parseDouble(parts[21]);
            for (int i = 0; i < LINE_HISTORY; i++) game.history[i] = lineId(parts[22 + i]);
            if (!(game.length > 0) || Double.isInfinite(game.length)
                || !(game.thickness > 0) || Double.isInfinite(game.thickness)
                || game.turn < 0 || game.stage < 0 || game.stage > game.turn
                || game.historyCursor < 0 || game.historyCursor >= LINE_HISTORY) return null;
            return game;
        } catch (IllegalArgumentException invalidSave) {
            return null;
        }
    }

    private static int lineId(String field) {
        int value = Integer.parseInt(field);
        return value >= 0 && value < Lines.ALL.length ? value : -1;
    }
}
