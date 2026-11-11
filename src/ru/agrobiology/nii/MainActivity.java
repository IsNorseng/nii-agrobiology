package ru.agrobiology.nii;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import java.util.ArrayList;
import java.util.Locale;

public final class MainActivity extends Activity {
    private Instrument instrument;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().setBackgroundDrawableResource(android.R.color.black);
        instrument = new Instrument(this);
        setContentView(instrument);
        immersive();
    }

    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) immersive();
    }

    @SuppressWarnings("deprecation")
    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override public void onBackPressed() {
        if (!instrument.back()) super.onBackPressed();
    }

    private static final class Instrument extends View {
        private static final int BG = 0xff302c2b;
        private static final int BG_GRID = 0xff3b3430;
        private static final int CASE_EDGE = 0xff4a4036;
        private static final int CASE_SHADOW = 0xffa38e6b;
        private static final int CASE = 0xffddc9a0;
        private static final int CASE_LIGHT = 0xfff1e2bd;
        private static final int PAPER = 0xfff2e6ca;
        private static final int INK = 0xff302b27;
        private static final int SUBTLE = 0xff66574a;
        private static final int RED = 0xff823f38;
        private static final int RED_DARK = 0xff542e2c;
        private static final int CRT_EDGE = 0xff3c4939;
        private static final int CRT = 0xffacb88d;
        private static final int CRT_LIGHT = 0xffbbc69d;
        private static final int CRT_RULE = 0xff8f9e76;
        private static final int CRT_INK = 0xff374535;
        private static final int GOLD = 0xffd5ad60;
        private static final float WIDTH = 360, HEIGHT = 680;
        private static final Typeface MONO = Typeface.create("monospace", Typeface.BOLD);
        private static final Typeface BODY = Typeface.create("sans-serif", Typeface.NORMAL);
        private static final Typeface BOLD = Typeface.create("sans-serif", Typeface.BOLD);
        private static final Typeface HEAVY = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        private static final String[] LABELS = {"Полить", "Подкормить", "Натереть", "Побрить"};
        private static final String[] INDEX = {"01", "02", "03", "04", "05"};
        private static final String[] TRAITS = {"Коллекционный блеск", "Скорый рост и ворс",
                "Солидная толщина", "Отзывчива на подкормку", "Особая кривизна"};
        private static final RectF ART_BOUNDS = new RectF(33, 211, 327, 424);
        private static final RectF BUBBLE = new RectF(28, 87, 332, 177);
        private static final RectF NEW_RUN = new RectF(26, 601, 151, 642);
        private static final RectF LAST_REPORT = new RectF(154, 601, 226, 642);
        private static final RectF CONTINUE = new RectF(38, 496, 322, 550);
        private static final RectF CONFIRM = new RectF(182, 389, 320, 440);
        private static final RectF CANCEL = new RectF(40, 389, 172, 440);
        private static final RectF RETURN = new RectF(26, 594, 174, 637);
        private static final int CONTINUE_ID = 20, NEW_ID = 21, CONFIRM_ID = 22;
        private static final int CANCEL_ID = 23, RETURN_ID = 24, CLOSE_LINE_ID = 25;
        private static final int LAST_REPORT_ID = 26;

        private final SharedPreferences prefs;
        private final Paint paint = new Paint();
        private final Path path = new Path();
        private final ProduceArt art = new ProduceArt();
        private final float[] records = new float[5];
        private final long start = SystemClock.uptimeMillis();
        private Game game;
        private boolean selecting, confirming, readingReport;
        private Report lastReport;
        private String dismissed;
        private int pressed = -1, animAction = -1;
        private long actionAt = -1000;
        private float scale = 1, left, top, downX, downY;
        private boolean trackBubble;

        Instrument(Context context) {
            super(context);
            setFocusable(true);
            setClickable(true);
            prefs = context.getSharedPreferences("agro_03", Context.MODE_PRIVATE);
            game = Game.restore(prefs.getString("experiment", ""));
            selecting = game == null;
            if (game != null) lastReport = Report.load(prefs, game.vegetable);
            dismissed = prefs.getString("dismissed", "");
            for (int i = 0; i < records.length; i++) {
                records[i] = prefs.getFloat("record_" + i, 0);
            }
        }

        private void save() {
            save(prefs.edit());
        }

        private void save(SharedPreferences.Editor editor) {
            if (game == null) return;
            int vegetable = game.vegetable;
            records[vegetable] = Math.max(records[vegetable], (float) game.length);
            editor.putString("experiment", game.save()).putString("dismissed", dismissed)
                    .putFloat("record_" + vegetable, records[vegetable]).apply();
        }

        @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
            scale = Math.min(w / WIDTH, h / HEIGHT);
            left = (w - WIDTH * scale) / 2;
            top = (h - HEIGHT * scale) / 2;
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            long now = SystemClock.uptimeMillis();
            c.drawColor(BG);
            float step = 12 * scale;
            if (step >= 1) {
                for (float y = 3; y < getHeight(); y += step) {
                    for (float x = 3; x < getWidth(); x += step) {
                        rect(c, x, y, x + scale, y + scale, BG_GRID);
                    }
                }
            }
            c.save();
            c.translate(left, top);
            c.scale(scale, scale);
            drawCase(c);
            if (selecting) {
                drawSelection(c);
            } else {
                drawExperiment(c, now);
                if (game.pendingReport()) drawReport(c, new Report(game), false);
                else if (readingReport) drawReport(c, lastReport, true);
                else if (confirming) drawConfirm(c);
            }
            c.restore();
            if (!selecting && !confirming && !readingReport && !game.pendingReport()) {
                postInvalidateDelayed(now - actionAt < 570 ? 17 : 140);
            }
        }

        private void drawCase(Canvas c) {
            cut(c, 9, 18, 351, 669, 15, 0xff211d1d);
            cut(c, 12, 14, 348, 660, 14, CASE_EDGE);
            cut(c, 15, 16, 345, 655, 13, CASE_SHADOW);
            cut(c, 16, 17, 344, 647, 12, CASE);
            rect(c, 29, 19, 331, 22, CASE_LIGHT);
            rect(c, 17, 32, 19, 630, CASE_LIGHT);
            rect(c, 341, 32, 343, 630, 0xffb9a37b);
            rect(c, 29, 644, 331, 647, 0xffc5ae83);
            txt(c, "НИИ АГРОБИОЛОГИИ", 30, 52, 23, HEAVY, INK, Paint.Align.LEFT);
            txt(c, "СССР", 31, 72, 13, MONO, RED, Paint.Align.LEFT);
            rect(c, 68, 62, 70, 72, RED);
            txt(c, "КАРМАННЫЙ БИОБЛОК", 77, 72, 10, MONO, SUBTLE, Paint.Align.LEFT);
            cut(c, 294, 33, 330, 69, 5, RED_DARK);
            cut(c, 296, 34, 328, 66, 4, RED);
            seal(c, 300, 38, 24, CASE_LIGHT);
            rect(c, 28, 78, 332, 80, CASE_SHADOW);
            rect(c, 28, 80, 332, 81, CASE_LIGHT);
        }

        private void drawSelection(Canvas c) {
            cut(c, 28, 95, 332, 219, 6, CASE_EDGE);
            cut(c, 30, 97, 330, 216, 5, CRT_EDGE);
            cut(c, 34, 101, 326, 212, 4, CRT);
            rect(c, 43, 112, 51, 120, RED);
            txt(c, "ПРИЁМ ОБРАЗЦОВ", 58, 120, 10, MONO, CRT_INK, Paint.Align.LEFT);
            txt(c, "ОТД. 03", 317, 120, 10, MONO, CRT_INK, Paint.Align.RIGHT);
            rect(c, 43, 128, 317, 129, CRT_RULE);
            txt(c, "Выберите овощ", 43, 157, 26, HEAVY, INK, Paint.Align.LEFT);
            paragraph(c, "Подопытный будет передан вам на ответственное хранение.",
                    43, 179, 261, 13, 16, BODY, CRT_INK);
            for (int i = 0; i < 5; i++) {
                float y = 231 + i * 70;
                float offset = pressed == 10 + i ? 2 : 0;
                cut(c, 28, y + 3, 332, y + 66, 5, CASE_SHADOW);
                cut(c, 28, y + offset, 332, y + 62 + offset, 5, CASE_EDGE);
                cut(c, 30, y + 2 + offset, 330, y + 60 + offset, 4, PAPER);
                cut(c, 35, y + 7 + offset, 87, y + 55 + offset, 4, CRT_EDGE);
                cut(c, 37, y + 9 + offset, 85, y + 53 + offset, 3, CRT);
                art.icon(c, new RectF(39, y + 9 + offset, 83, y + 53 + offset), i);
                txt(c, Game.NAMES[i], 97, y + 26 + offset, 20, HEAVY, INK, Paint.Align.LEFT);
                txt(c, TRAITS[i], 97, y + 44 + offset, 12, BODY, SUBTLE, Paint.Align.LEFT);
                String right = records[i] > 0 ? cm(records[i]) : INDEX[i];
                txt(c, right, 309, y + 24 + offset, 9.5f, MONO, SUBTLE, Paint.Align.RIGHT);
                chevron(c, 308, y + 33 + offset, RED);
            }
            if (game != null) {
                txt(c, "‹  К ОБРАЗЦУ", 31, 622, 11, MONO, RED, Paint.Align.LEFT);
            } else {
                txt(c, "ПАРТИЯ № 03   /   ДОПУСК ВЫДАН", 31, 622, 9.5f, MONO, SUBTLE,
                        Paint.Align.LEFT);
            }
            serial(c, 620);
        }

        private void drawExperiment(Canvas c, long now) {
            drawBubble(c);
            cut(c, 27, 181, 333, 461, 9, CASE_EDGE);
            cut(c, 29, 183, 331, 458, 8, 0xff53604b);
            cut(c, 33, 187, 327, 453, 6, CRT_EDGE);
            cut(c, 36, 190, 324, 450, 5, CRT);
            c.save();
            c.clipRect(38, 192, 322, 448);
            for (int y = 194; y < 451; y += 4) {
                rect(c, 38, y, 322, y + 1, 0x0c384536);
            }
            for (int x = 47; x < 323; x += 12) {
                for (int y = 220; y < 427; y += 12) {
                    rect(c, x, y, x + 1, y + 1, 0x2b455642);
                }
            }
            rect(c, 41, 212, 319, 213, CRT_RULE);
            rect(c, 41, 426, 319, 427, CRT_RULE);
            rect(c, 43, 220, 44, 420, CRT_RULE);
            for (int y = 223; y < 420; y += 12) {
                rect(c, 44, y, y % 24 == 7 ? 52 : 48, y + 1, CRT_RULE);
            }
            txt(c, Game.NAMES[game.vegetable].toUpperCase(Locale.ROOT), 44, 205, 10, MONO,
                    CRT_INK, Paint.Align.LEFT);
            txt(c, "ОБР. " + INDEX[game.vegetable], 315, 205, 10, MONO,
                    CRT_INK, Paint.Align.RIGHT);
            art.draw(c, ART_BOUNDS, game, now - start, animAction, now - actionAt);
            rect(c, 44, 435, 50, 441, RED);
            txt(c, stateLabel(), 55, 441, 10, MONO, CRT_INK, Paint.Align.LEFT);
            txt(c, "●  В ОПЫТЕ", 315, 441, 9.5f, MONO, CRT_INK, Paint.Align.RIGHT);
            c.restore();
            drawStats(c);
            for (int i = 0; i < LABELS.length; i++) drawAction(c, i);
            rect(c, 32, 618, 42, 620, RED);
            rect(c, 36, 614, 38, 624, RED);
            txt(c, "НОВЫЙ ОБРАЗЕЦ", 49, 624, 10, MONO, RED, Paint.Align.LEFT);
            if (lastReport != null) {
                cut(c, 155, 608, 209, 633, 3, PAPER);
                txt(c, "АКТ", 182, 624, 10, MONO, RED, Paint.Align.CENTER);
            }
            serial(c, 622);
        }

        private void drawBubble(Canvas c) {
            boolean visible = lineVisible();
            cut(c, 28, 88, 332, 172, 6, CASE_SHADOW);
            cut(c, 28, 86, 332, 169, 6, CASE_EDGE);
            cut(c, 30, 88, 330, 167, 5, PAPER);
            rect(c, 53, 167, 66, 173, CASE_EDGE);
            rect(c, 55, 167, 65, 171, PAPER);
            rect(c, 55, 173, 63, 176, CASE_EDGE);
            rect(c, 56, 171, 61, 174, PAPER);
            cut(c, 36, 93, 62, 119, 3, visible && !game.commissionSpeaking() ? CRT_EDGE : RED_DARK);
            if (visible && !game.commissionSpeaking()) {
                cut(c, 38, 95, 60, 117, 2, CRT);
                art.icon(c, new RectF(38, 95, 60, 117), game.vegetable);
            } else {
                seal(c, 39, 96, 20, CASE_LIGHT);
            }
            String who = !visible ? "СВЯЗЬ"
                    : game.commissionSpeaking() ? "КОМИССИЯ" : "ОВОЩ";
            txt(c, who, 69, 107, 11, MONO, RED, Paint.Align.LEFT);
            rect(c, 68, 112, 322, 113, 0xffd8c7a5);
            if (visible) {
                txt(c, "×", 319, 108, 15, BOLD, SUBTLE, Paint.Align.RIGHT);
                paragraph(c, game.lineText(), 40, 134, 281, 13, 14, BODY, INK);
            } else {
                txt(c, "Ожидание сообщения…", 40, 140, 13, BODY, SUBTLE, Paint.Align.LEFT);
            }
        }

        private void drawStats(Canvas c) {
            cut(c, 28, 467, 332, 521, 4, CASE_SHADOW);
            cut(c, 30, 468, 330, 519, 3, PAPER);
            rect(c, 106, 475, 107, 507, 0xffd8c7a5);
            rect(c, 181, 475, 182, 507, 0xffd8c7a5);
            rect(c, 256, 475, 257, 507, 0xffd8c7a5);
            metric(c, 39, "ДЛИНА", game.length);
            metric(c, 115, "ТОЛЩИНА", game.thickness);
            metric(c, 190, "ЦЕЛЬ", game.goal());
            metric(c, 265, "РЕКОРД", records[game.vegetable]);
            rect(c, 36, 512, 324, 515, 0xffdfcead);
            float fraction = (float) Math.min(1, game.length / game.goal());
            rect(c, 36, 512, 36 + 288 * fraction, 515, RED);
        }

        private void metric(Canvas c, float x, String label, double value) {
            txt(c, label, x, 484, 9, MONO, SUBTLE, Paint.Align.LEFT);
            String number = number(value);
            float size = number.length() > 6 ? 12 : number.length() > 4 ? 14 : 16;
            txt(c, number, x, 503, size, MONO, INK, Paint.Align.LEFT);
            txt(c, "см", x + measure(number, size, MONO) + 3, 502, 8.5f, BOLD, SUBTLE,
                    Paint.Align.LEFT);
        }

        private void drawAction(Canvas c, int i) {
            float x = 28 + 77 * i;
            float y = pressed == i ? 537 : 533;
            cut(c, x, 536, x + 73, 597, 6, CASE_EDGE);
            cut(c, x + 1, y, x + 72, y + 57, 5, RED_DARK);
            cut(c, x + 3, y + 2, x + 70, y + 54, 4, RED);
            rect(c, x + 8, y + 3, x + 65, y + 5, 0xffa85e4d);
            actionIcon(c, i, x + 36, y + 19, PAPER);
            float labelSize = Math.min(12, 61 * 12 / measure(LABELS[i], 12, BOLD));
            txt(c, LABELS[i], x + 36.5f, y + 45, labelSize, BOLD, PAPER, Paint.Align.CENTER);
        }

        private void drawReport(Canvas c, Report report, boolean archived) {
            c.drawColor(0xba241f21);
            cut(c, 21, 117, 339, 572, 10, 0xff211d1d);
            cut(c, 23, 114, 337, 567, 8, RED_DARK);
            cut(c, 26, 117, 334, 564, 7, PAPER);
            cut(c, 37, 128, 323, 222, 5, RED);
            seal(c, 164, 135, 32, PAPER);
            txt(c, "КОМИССИОННОЕ", 180, 189, 21, HEAVY, PAPER, Paint.Align.CENTER);
            txt(c, "ИЗМЕРЕНИЕ", 180, 211, 22, HEAVY, PAPER, Paint.Align.CENTER);
            txt(c, "НОРМАТИВ " + report.goal + " СМ  /  ОБР. " + INDEX[game.vegetable],
                    39, 245, 11, MONO, RED, Paint.Align.LEFT);
            txt(c, "ДЛИНА", 39, 269, 10, MONO, SUBTLE, Paint.Align.LEFT);
            txt(c, "ТОЛЩИНА", 184, 269, 10, MONO, SUBTLE, Paint.Align.LEFT);
            txt(c, cm(report.length), 39, 298, 25, HEAVY, INK, Paint.Align.LEFT);
            txt(c, cm(report.thickness), 184, 298, 25, HEAVY, INK, Paint.Align.LEFT);
            rect(c, 39, 309, 320, 310, CASE_SHADOW);
            txt(c, "Поверхность", 39, 331, 12, BOLD, SUBTLE, Paint.Align.LEFT);
            txt(c, report.notes[0], 320, 331, 13, BODY, INK, Paint.Align.RIGHT);
            txt(c, "Волосистость", 39, 352, 12, BOLD, SUBTLE, Paint.Align.LEFT);
            txt(c, report.notes[1], 320, 352, 13, BODY, INK, Paint.Align.RIGHT);
            cut(c, 38, 368, 322, 484, 4, 0xffe6d5ad);
            txt(c, "ЗАКЛЮЧЕНИЕ", 49, 389, 10, MONO, RED, Paint.Align.LEFT);
            float size = 16;
            ArrayList<String> lines = wrap(report.notes[2], 261, size, BOLD);
            while (lines.size() * (size + 2) > 81 && size > 12) {
                size -= 1;
                lines = wrap(report.notes[2], 261, size, BOLD);
            }
            drawLines(c, lines, 49, 407, size, size + 2, BOLD, INK);
            button(c, CONTINUE, archived ? "К ОБРАЗЦУ" : "ПРОДОЛЖИТЬ ЭКСПЕРИМЕНТ",
                    pressed == CONTINUE_ID);
        }

        private void drawConfirm(Canvas c) {
            c.drawColor(0xba241f21);
            cut(c, 23, 232, 337, 462, 8, RED_DARK);
            cut(c, 26, 235, 334, 459, 7, PAPER);
            txt(c, "СМЕНА ОБРАЗЦА", 40, 267, 11, MONO, RED, Paint.Align.LEFT);
            txt(c, "Начать новый опыт?", 40, 300, 24, HEAVY, INK, Paint.Align.LEFT);
            paragraph(c, "Текущий образец будет списан после выбора нового. Рекорд сохранится.",
                    40, 327, 274, 14, 18, BODY, INK);
            button(c, CANCEL, "К ОБРАЗЦУ", pressed == CANCEL_ID);
            button(c, CONFIRM, "К ВЫБОРУ", pressed == CONFIRM_ID);
        }

        private void button(Canvas c, RectF r, String label, boolean down) {
            float offset = down ? 2 : 0;
            cut(c, r.left, r.top + 2, r.right, r.bottom + 1, 5, RED_DARK);
            cut(c, r.left + 1, r.top + offset, r.right - 1, r.bottom - 2 + offset, 5, RED);
            txt(c, label, r.centerX(), r.centerY() + 4 + offset, 13, MONO, PAPER, Paint.Align.CENTER);
        }

        private void serial(Canvas c, int y) {
            txt(c, "БИОБЛОК 0.3", 279, y, 8.5f, MONO, SUBTLE, Paint.Align.RIGHT);
            for (int i = 0; i < 4; i++) {
                rect(c, 288 + i * 9, y - 8, 293 + i * 9, y + 2, CASE_SHADOW);
                rect(c, 288 + i * 9, y - 8, 291 + i * 9, y + 1, CASE_EDGE);
            }
        }

        private String stateLabel() {
            if (game.has("flies")) return "ОБНАРУЖЕНЫ МУХИ";
            if (game.has("film")) return "ПОЯВИЛСЯ НАЛЁТ";
            if (game.has("polished")) return "ПОДОЗРИТЕЛЬНО ГЛАДКИЙ";
            if (game.has("irritated")) return "РАЗДРАЖЕНИЕ";
            if (game.has("warped")) return "ДЕФОРМАЦИЯ";
            if (game.has("overfed")) return "ПЕРЕКОРМ";
            if (game.has("wet")) return "МОКРЫЙ ОБРАЗЕЦ";
            if (game.has("dry")) return "СУХОЙ ОБРАЗЕЦ";
            if (game.has("fuzz")) return "ИЗБЫТОЧНЫЙ ВОРС";
            if (game.has("dirty")) return "ЗАГРЯЗНЕНИЕ";
            if (game.has("shaved")) return "СВЕЖЕВЫБРИТ";
            if (game.has("shiny")) return "ПОВЕРХНОСТЬ БЛЕСТИТ";
            if (game.has("thick")) return "УТОЛЩЕНИЕ";
            if (game.has("long")) return "НЕГАБАРИТНЫЙ";
            if (game.has("thin")) return "ТОНКИЙ ОБРАЗЕЦ";
            if (game.has("clean")) return "ПОВЕРХНОСТЬ ЧИСТАЯ";
            return "СОСТОЯНИЕ В НОРМЕ";
        }

        private boolean lineVisible() {
            String line = game.lineText();
            return line != null && !line.isEmpty() && !line.equals(dismissed);
        }

        private static String number(double value) {
            if (Math.abs(value - Math.rint(value)) < 0.01) return Long.toString(Math.round(value));
            return String.format(Locale.ROOT, "%.1f", value).replace('.', ',');
        }

        private static String cm(double value) {
            return number(value) + " см";
        }

        private void rect(Canvas c, float x, float y, float r, float b, int color) {
            paint.setAntiAlias(false);
            paint.setColor(color);
            c.drawRect(x, y, r, b, paint);
        }

        private void cut(Canvas c, float x, float y, float r, float b, float corner, int color) {
            path.reset();
            path.moveTo(x + corner, y);
            path.lineTo(r - corner, y);
            path.lineTo(r - corner, y + corner);
            path.lineTo(r, y + corner);
            path.lineTo(r, b - corner);
            path.lineTo(r - corner, b - corner);
            path.lineTo(r - corner, b);
            path.lineTo(x + corner, b);
            path.lineTo(x + corner, b - corner);
            path.lineTo(x, b - corner);
            path.lineTo(x, y + corner);
            path.lineTo(x + corner, y + corner);
            path.close();
            paint.setAntiAlias(false);
            paint.setColor(color);
            c.drawPath(path, paint);
        }

        private void txt(Canvas c, String value, float x, float baseline, float size, Typeface face,
                         int color, Paint.Align align) {
            paint.setAntiAlias(true);
            paint.setTypeface(face);
            paint.setTextSize(size);
            paint.setTextAlign(align);
            paint.setColor(color);
            c.drawText(value, x, baseline, paint);
        }

        private float measure(String value, float size, Typeface face) {
            paint.setTypeface(face);
            paint.setTextSize(size);
            return paint.measureText(value);
        }

        private void paragraph(Canvas c, String value, float x, float baseline, float width,
                               float size, float leading, Typeface face, int color) {
            drawLines(c, wrap(value, width, size, face), x, baseline, size, leading, face, color);
        }

        private ArrayList<String> wrap(String value, float width, float size, Typeface face) {
            ArrayList<String> lines = new ArrayList<>();
            for (String block : value.split("\n")) {
                String line = "";
                for (String word : block.split(" +")) {
                    String next = line.isEmpty() ? word : line + " " + word;
                    if (!line.isEmpty() && measure(next, size, face) > width) {
                        lines.add(line);
                        line = word;
                    } else {
                        line = next;
                    }
                }
                lines.add(line);
            }
            return lines;
        }

        private void drawLines(Canvas c, ArrayList<String> lines, float x, float baseline,
                               float size, float leading, Typeface face, int color) {
            for (String line : lines) {
                txt(c, line, x, baseline, size, face, color, Paint.Align.LEFT);
                baseline += leading;
            }
        }

        private void seal(Canvas c, float x, float y, float s, int color) {
            float u = s / 12;
            pix(c, x, y, u, color, 2, 0, 8, 1);
            pix(c, x, y, u, color, 0, 2, 1, 8);
            pix(c, x, y, u, color, 11, 2, 1, 8);
            pix(c, x, y, u, color, 2, 11, 8, 1);
            pix(c, x, y, u, color, 1, 1, 1, 1);
            pix(c, x, y, u, color, 10, 1, 1, 1);
            pix(c, x, y, u, color, 1, 10, 1, 1);
            pix(c, x, y, u, color, 10, 10, 1, 1);
            pix(c, x, y, u, color, 3, 8, 6, 1);
            pix(c, x, y, u, color, 3, 4, 2, 3);
            pix(c, x, y, u, color, 5, 3, 2, 5);
            pix(c, x, y, u, color, 7, 4, 2, 3);
        }

        private void chevron(Canvas c, float x, float y, int color) {
            rect(c, x, y, x + 2, y + 2, color);
            rect(c, x + 2, y + 2, x + 4, y + 4, color);
            rect(c, x + 4, y + 4, x + 6, y + 6, color);
            rect(c, x + 2, y + 6, x + 4, y + 8, color);
            rect(c, x, y + 8, x + 2, y + 10, color);
        }

        private void pix(Canvas c, float x, float y, float unit, int color,
                         int px, int py, int w, int h) {
            rect(c, x + px * unit, y + py * unit, x + (px + w) * unit, y + (py + h) * unit, color);
        }

        private void actionIcon(Canvas c, int i, float center, float mid, int color) {
            float x = center - 12, y = mid - 10, u = 2;
            if (i == 0) {
                pix(c, x, y, u, color, 5, 0, 2, 2);
                pix(c, x, y, u, color, 4, 2, 4, 2);
                pix(c, x, y, u, color, 3, 4, 6, 2);
                pix(c, x, y, u, color, 2, 6, 8, 2);
                pix(c, x, y, u, color, 3, 8, 6, 1);
                pix(c, x, y, u, color, 4, 9, 4, 1);
                pix(c, x, y, u, RED, 4, 5, 1, 2);
            } else if (i == 1) {
                pix(c, x, y, u, color, 3, 0, 6, 2);
                pix(c, x, y, u, color, 2, 3, 8, 6);
                pix(c, x, y, u, color, 3, 9, 6, 1);
                pix(c, x, y, u, RED, 4, 5, 4, 1);
                pix(c, x, y, u, RED, 5, 4, 2, 3);
            } else if (i == 2) {
                pix(c, x, y, u, color, 5, 0, 2, 10);
                pix(c, x, y, u, color, 2, 3, 8, 4);
                pix(c, x, y, u, color, 0, 4, 12, 2);
                pix(c, x, y, u, GOLD, 10, 0, 2, 2);
            } else {
                pix(c, x, y, u, color, 1, 1, 10, 2);
                pix(c, x, y, u, color, 2, 3, 8, 1);
                pix(c, x, y, u, color, 5, 4, 2, 6);
                pix(c, x, y, u, color, 4, 8, 4, 2);
            }
        }

        private int hit(float x, float y) {
            if (selecting) {
                if (game != null && RETURN.contains(x, y)) return RETURN_ID;
                if (x >= 28 && x <= 332 && y >= 231) {
                    int i = (int) ((y - 231) / 70);
                    if (i < 5 && y < 296 + 70 * i) return 10 + i;
                }
                return -1;
            }
            if (game.pendingReport() || readingReport) return CONTINUE.contains(x, y) ? CONTINUE_ID : -1;
            if (confirming) {
                if (CONFIRM.contains(x, y)) return CONFIRM_ID;
                return CANCEL.contains(x, y) ? CANCEL_ID : -1;
            }
            if (NEW_RUN.contains(x, y)) return NEW_ID;
            if (lastReport != null && LAST_REPORT.contains(x, y)) return LAST_REPORT_ID;
            if (lineVisible() && x >= 293 && y >= 87 && x <= 333 && y <= 123) return CLOSE_LINE_ID;
            if (x >= 28 && x < 332 && y >= 528 && y <= 598) return (int) ((x - 28) / 77);
            return -1;
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            float x = (event.getX() - left) / scale, y = (event.getY() - top) / scale;
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = x;
                    downY = y;
                    pressed = hit(x, y);
                    trackBubble = !selecting && !confirming && !readingReport && !game.pendingReport()
                            && lineVisible() && BUBBLE.contains(x, y);
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                    int id = pressed;
                    pressed = -1;
                    boolean swipe = trackBubble && Math.max(Math.abs(x - downX),
                            Math.abs(y - downY)) > 35;
                    trackBubble = false;
                    if (swipe) {
                        activate(CLOSE_LINE_ID);
                    } else if (id >= 0 && hit(x, y) == id) {
                        performClick();
                        activate(id);
                    }
                    invalidate();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    pressed = -1;
                    trackBubble = false;
                    invalidate();
                    return true;
                default:
                    return true;
            }
        }

        @Override public boolean performClick() {
            super.performClick();
            return true;
        }

        private void activate(int id) {
            if (id >= 0 && id < 4) {
                String previous = game.lineText();
                game.act(id);
                if (!game.lineText().equals(previous)) dismissed = "";
                animAction = id;
                actionAt = SystemClock.uptimeMillis();
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                save();
            } else if (id >= 10 && id < 15) {
                game = new Game(id - 10);
                lastReport = Report.load(prefs, game.vegetable);
                selecting = false;
                dismissed = "";
                animAction = -1;
                save();
            } else if (id == CONTINUE_ID) {
                if (readingReport) {
                    readingReport = false;
                } else {
                    lastReport = new Report(game);
                    SharedPreferences.Editor editor = prefs.edit();
                    lastReport.save(editor, game.vegetable);
                    game.continueExperiment();
                    dismissed = "";
                    save(editor);
                }
            } else if (id == LAST_REPORT_ID) {
                readingReport = true;
            } else if (id == NEW_ID) {
                confirming = true;
            } else if (id == CONFIRM_ID) {
                confirming = false;
                selecting = true;
            } else if (id == RETURN_ID) {
                selecting = false;
            } else if (id == CANCEL_ID) {
                confirming = false;
            } else if (id == CLOSE_LINE_ID) {
                dismissed = game.lineText();
                save();
            }
        }

        private boolean back() {
            if (readingReport) {
                readingReport = false;
            } else if (confirming) {
                confirming = false;
            } else if (selecting && game != null) {
                selecting = false;
            } else {
                return false;
            }
            invalidate();
            return true;
        }

        private static final class Report {
            final int goal;
            final float length, thickness;
            final String[] notes;

            Report(Game game) {
                this(game.goal(), (float) game.length, (float) game.thickness, game.report());
            }

            Report(int goal, float length, float thickness, String[] notes) {
                this.goal = goal;
                this.length = length;
                this.thickness = thickness;
                this.notes = notes;
            }

            static Report load(SharedPreferences prefs, int vegetable) {
                String key = "report_" + vegetable + "_";
                int goal = prefs.getInt(key + "goal", 0);
                if (goal == 0) return null;
                return new Report(goal, prefs.getFloat(key + "length", 0),
                        prefs.getFloat(key + "thickness", 0),
                        new String[] {prefs.getString(key + "surface", ""),
                                prefs.getString(key + "hair", ""),
                                prefs.getString(key + "conclusion", "")});
            }

            void save(SharedPreferences.Editor editor, int vegetable) {
                String key = "report_" + vegetable + "_";
                editor.putInt(key + "goal", goal).putFloat(key + "length", length)
                        .putFloat(key + "thickness", thickness).putString(key + "surface", notes[0])
                        .putString(key + "hair", notes[1]).putString(key + "conclusion", notes[2]);
            }
        }
    }
}
