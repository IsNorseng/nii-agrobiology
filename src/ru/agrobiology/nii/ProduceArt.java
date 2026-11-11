package ru.agrobiology.nii;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Pixel shapes and independent surface layers for the five specimens. */
final class ProduceArt {
    private static final int[] HEIGHT = {44, 49, 43, 50, 48};
    private static final int[] WIDTH = {28, 20, 31, 25, 18};
    private static final double[] START_WIDTH = {3.6, 2.8, 4.0, 2.6, 3.0};
    private static final int SPRITE_X = 70;
    private static final int SPRITE_Y = 15;

    // Outline, shadow, skin, light, highlight, and natural markings.
    private static final int[][] SKIN = {
        {0xFF332A37, 0xFF4C3B54, 0xFF6C506E, 0xFF916E8A, 0xFFB38D9F, 0xFF654862},
        {0xFF283B2D, 0xFF3A5339, 0xFF5F7646, 0xFF82915A, 0xFFA6AE6F, 0xFF50663F},
        {0xFF2D3A2F, 0xFF40553E, 0xFF637955, 0xFF859363, 0xFFA8B383, 0xFF526A48},
        {0xFF513727, 0xFF9B5F3E, 0xFFC47D47, 0xFFDC995B, 0xFFEAB477, 0xFF9F603D},
        {0xFF57462C, 0xFF92733B, 0xFFC6A258, 0xFFDFC178, 0xFFE8D19A, 0xFFAD8849}
    };
    private static final int[] STEM = {
        0, 0xFF29382A, 0xFF45563A, 0xFF687948, 0xFF9DA271, 0xFF72563B, 0xFFAA8652
    };
    private static final String[][] TOPS = {
        {"....111....", "...1231....", "...1231....", "...12311...", "..1123411..", "11233343211", "12343333221", "1223333221.", ".11.121.11.", ".....1....."},
        {"...11..", "..1221.", "..1231.", ".12331.", "123321.", ".1221..", "..11..."},
        {".1111....", "123341...", "1233441..", "1223341..", ".122341..", "..123341.", "..1223441", "...122331", "....1111."},
        {"........11.........", "..111..1331...11...", ".13331124331.1341..", "12343312343113431..", "122343123431343321.", ".12234312341433211.", "..122344234143221..", "...1223433433221...", "....11243343221....", ".....112333211.....", ".......12321.......", ".......12321.......", "........121........"},
        {".11111..", "156551..", "155661..", ".156651.", "..155651", "..156651", "...15521", "....111."}
    };

    private final Paint paint = new Paint();
    private final Paint spritePaint = new Paint();
    private final Bitmap liveSprite = newSprite();
    private final Canvas spriteCanvas = new Canvas(liveSprite);
    private final Bitmap[] iconSprites = new Bitmap[HEIGHT.length];

    ProduceArt() {
        spritePaint.setFilterBitmap(false);
    }

    void draw(Canvas canvas, RectF bounds, Game game, long timeMs, int action, long actionAgeMs) {
        int kind = game.vegetable;
        int height = HEIGHT[kind] + (int) Math.round(16 * Math.log1p(Math.max(0, game.length - 10) / 23));
        int width = WIDTH[kind] + (int) Math.round((game.thickness - START_WIDTH[kind]) * 2.1);
        Shape shape = new Shape(kind, clamp(height, HEIGHT[kind] - 1, 68), clamp(width, 14, 51), game.warp);
        float zoom = Math.min(bounds.width() / 103f, bounds.height() / 90f);
        float motion = action >= 0 && actionAgeMs >= 0 && actionAgeMs < 420 ? 1 - actionAgeMs / 420f : 0;
        float lean = (float) Math.sin(actionAgeMs * .023) * motion * (action == 2 ? 5f : 3f);
        int bob = (int) Math.round(Math.sin(timeMs / 420.0) * .7);
        int floor = shape.height / 2 + 5;
        liveSprite.eraseColor(0);
        renderSprite(spriteCanvas, shape, game, timeMs);

        canvas.save();
        canvas.clipRect(bounds);
        canvas.translate(Math.round(bounds.centerX()), Math.round(bounds.centerY() + 8));
        canvas.scale(zoom, zoom);
        shadow(canvas, shape, floor);

        canvas.save();
        canvas.rotate(lean);
        canvas.translate(0, -shape.height / 2 - bob - (action == 1 ? Math.round(2 * motion) : 0));
        canvas.drawBitmap(liveSprite, -SPRITE_X, -SPRITE_Y, spritePaint);
        canvas.restore();

        if (action >= 0 && actionAgeMs >= 0 && actionAgeMs < 570) {
            particles(canvas, shape, action, actionAgeMs / 570f);
        }
        canvas.restore();
    }

    void icon(Canvas canvas, RectF bounds, int vegetable) {
        int height = HEIGHT[vegetable];
        if (iconSprites[vegetable] == null) {
            Bitmap icon = newSprite();
            renderSprite(new Canvas(icon), new Shape(vegetable, height, WIDTH[vegetable], 0), null, 0);
            iconSprites[vegetable] = icon;
        }
        float zoom = Math.min(bounds.width() / 43f, bounds.height() / 64f);
        canvas.save();
        canvas.translate(bounds.centerX(), bounds.centerY() - (height - 7) * zoom / 2);
        canvas.scale(zoom, zoom);
        canvas.drawBitmap(iconSprites[vegetable], -SPRITE_X, -SPRITE_Y, spritePaint);
        canvas.restore();
    }

    private static Bitmap newSprite() {
        Bitmap sprite = Bitmap.createBitmap(SPRITE_X * 2, SPRITE_Y + 72, Bitmap.Config.ARGB_8888);
        sprite.setDensity(Bitmap.DENSITY_NONE);
        return sprite;
    }

    private void renderSprite(Canvas canvas, Shape shape, Game game, long timeMs) {
        canvas.save();
        canvas.translate(SPRITE_X, SPRITE_Y);
        specimen(canvas, shape, game, timeMs);
        canvas.restore();
    }

    private void shadow(Canvas canvas, Shape shape, int floor) {
        int span = shape.width / 2 + (shape.kind == 4 ? 12 : 7);
        for (int y = -3; y <= 3; y++) {
            int edge = span - Math.abs(y) * 3;
            rect(canvas, -edge, floor + y, edge * 2 + 1, 1, 0x22293427);
        }
        rect(canvas, -span + 6, floor - 1, (span - 6) * 2, 3, 0x24293427);
    }

    private void specimen(Canvas canvas, Shape shape, Game game, long timeMs) {
        int[] palette = SKIN[shape.kind];
        for (int y = 0; y < shape.height; y++) {
            int left = shape.left[y];
            int right = shape.right[y];
            rect(canvas, left - 1, y, right - left + 3, 1, palette[0]);
        }

        for (int y = 1; y < shape.height - 1; y++) {
            int left = Math.max(shape.left[y], Math.max(shape.left[y - 1], shape.left[y + 1]));
            int right = Math.min(shape.right[y], Math.min(shape.right[y - 1], shape.right[y + 1]));
            if (right < left) continue;

            int rowWidth = right - left + 1;
            rect(canvas, left, y, rowWidth, 1, palette[1]);
            int litLeft = left + rowWidth / 10;
            int litWidth = rowWidth - rowWidth / 4;
            rect(canvas, litLeft, y, litWidth, 1, palette[2]);

            int grain = hash(y, shape.kind) % 4;
            if (rowWidth > 5 && y < shape.height * .93) {
                int lightWidth = Math.max(1, rowWidth / 5 + (grain == 0 ? 1 : 0));
                rect(canvas, left + rowWidth / 6, y, lightWidth, 1, palette[3]);
                if (y > shape.height * .11 && y < shape.height * .64 && rowWidth > 10 && grain != 0) {
                    rect(canvas, left + rowWidth / 6, y, 1, 1, palette[4]);
                }
            }
            if (shape.kind == 4 && rowWidth > 6) {
                rect(canvas, right - rowWidth / 5, y, 1, 1, palette[3]);
            }
        }

        markings(canvas, shape, palette);
        if (game != null) surface(canvas, shape, game, timeMs);
        face(canvas, shape, game, timeMs);
        top(canvas, shape);
    }

    private void markings(Canvas canvas, Shape shape, int[] palette) {
        if (shape.kind == 3) {
            for (int y = 10; y < shape.height - 5; y += 7) {
                boolean right = y % 2 == 0;
                int x = right ? shape.right[y] - 5 : shape.left[y] + 2;
                tile(canvas, shape, x, y, 4, 1, palette[5]);
                tile(canvas, shape, x + (right ? 0 : 2), y + 1, 2, 1, palette[3]);
            }
            return;
        }
        int marks = shape.kind == 1 ? 42 : shape.kind == 2 ? 30 : 14;
        for (int i = 0; i < marks; i++) {
            int y = 4 + hash(i, 18) % (shape.height - 9);
            int x = skinX(shape, y, i, 36);
            if (y >= shape.height * .40 && y < shape.height * .63 && Math.abs(x - shape.center[y]) < 9) continue;
            tile(canvas, shape, x, y, 1, 1, i % 3 == 0 ? palette[4] : palette[5]);
            if (shape.kind == 1 && i % 3 == 0) tile(canvas, shape, x, y + 1, 1, 1, palette[1]);
        }
        if (shape.kind == 4) {
            int y = shape.height - 3;
            tile(canvas, shape, shape.center[y] - 2, y, 4, 2, palette[0]);
        }
    }

    private void top(Canvas canvas, Shape shape) {
        String[] rows = TOPS[shape.kind];
        int left = shape.center[0] - rows[0].length() / 2;
        int y = -rows.length + (shape.kind == 0 ? 6 : 2);
        for (String row : rows) {
            for (int x = 0; x < row.length(); x++) {
                if (row.charAt(x) != '.') rect(canvas, left + x, y, 1, 1, STEM[row.charAt(x) - '0']);
            }
            y++;
        }
    }

    private void face(Canvas canvas, Shape shape, Game game, long timeMs) {
        int y = Math.round(shape.height * .49f);
        int x = shape.center[y];
        int gap = clamp((shape.right[y] - shape.left[y]) / 4, 3, 6);
        int ink = 0xFF2E2C2A;
        int cheek = shape.kind == 0 ? 0xFFA67588 : 0xFFB98068;
        boolean irritated = game != null && game.has("irritated");
        boolean worried = game != null && (game.has("dry") || game.has("overfed") || game.has("wet"));
        boolean blink = timeMs % 3900 > 3755;
        int eyeHeight = blink ? 1 : irritated ? 2 : 3;

        tile(canvas, shape, x - gap - 1, y, 2, eyeHeight, ink);
        tile(canvas, shape, x + gap, y, 2, eyeHeight, ink);
        if (eyeHeight == 3) {
            tile(canvas, shape, x - gap - 1, y, 1, 1, 0xFFF0DDAD);
            tile(canvas, shape, x + gap, y, 1, 1, 0xFFF0DDAD);
        }
        tile(canvas, shape, x - gap - 3, y + 4, 2, 1, cheek);
        tile(canvas, shape, x + gap + 2, y + 4, 2, 1, cheek);

        if (irritated) {
            tile(canvas, shape, x - gap - 2, y - 3, 2, 1, ink);
            tile(canvas, shape, x + gap + 1, y - 3, 2, 1, ink);
            tile(canvas, shape, x - gap, y - 2, 1, 1, ink);
            tile(canvas, shape, x + gap, y - 2, 1, 1, ink);
            tile(canvas, shape, x - 2, y + 6, 5, 1, ink);
        } else if (worried) {
            tile(canvas, shape, x - 1, y + 5, 3, 2, ink);
            tile(canvas, shape, x, y + 6, 1, 1, cheek);
        } else {
            tile(canvas, shape, x - 2, y + 5, 1, 1, ink);
            tile(canvas, shape, x + 2, y + 5, 1, 1, ink);
            tile(canvas, shape, x - 1, y + 6, 3, 1, ink);
        }
    }

    private void surface(Canvas canvas, Shape shape, Game game, long timeMs) {
        if (game.has("dirty")) spots(canvas, shape, 13, 58, 0xFF775D3E, 0xFF594830);
        if (game.has("film")) spots(canvas, shape, 15, 81, 0xFF9A9C63, 0xFFB8AC75);
        if (game.has("irritated")) spots(canvas, shape, 11, 72, 0xFFAD685A, 0xFF874D49);
        if (game.has("overfed")) spots(canvas, shape, 7, 92, 0xFF89764E, 0xFFB29465);

        if (game.has("dry")) {
            for (int i = 0; i < 9; i++) {
                int y = 5 + hash(i, 13) % (shape.height - 12);
                int x = skinX(shape, y, i, 40);
                int ink = SKIN[shape.kind][1];
                tile(canvas, shape, x, y, 2, 1, ink);
                tile(canvas, shape, x + 1, y + 1, 1, 2, ink);
                tile(canvas, shape, x + 2, y + 3, 2, 1, ink);
            }
        }

        if (game.has("fuzz")) hairs(canvas, shape, game.fuzz);
        if (game.has("wet")) {
            for (int i = 0; i < 8; i++) {
                int y = 5 + hash(i, 55) % (shape.height - 11);
                int x = skinX(shape, y, i, 44);
                drop(canvas, x, y, 0xFF567778, 0xFFA8C4AC);
            }
        }

        boolean polished = game.has("polished");
        if (polished || game.has("shiny") || game.has("clean") || game.has("shaved")) {
            int y = shape.height / 4;
            int x = shape.left[y] + 3;
            int light = polished ? 0xFFF0E5BD : SKIN[shape.kind][4];
            tile(canvas, shape, x, y, polished ? 3 : 2, 6, light);
            if (polished || game.has("shiny")) {
                sparkle(canvas, shape.right[y + 1] + 2, y - 1, 3, light);
            }
            if (polished) {
                y = shape.height * 3 / 4;
                sparkle(canvas, shape.left[y] - 3, y, 3, light);
                tile(canvas, shape, shape.right[y] - 3, y, 2, 7, 0xFFC7BCA2);
            }
        }

        if (game.has("flies")) {
            for (int i = 0; i < 3; i++) {
                int y = shape.height * (i + 1) / 5;
                int edge = i % 2 == 0 ? shape.right[y] + 10 : shape.left[y] - 9;
                int x = edge + (int) Math.round(Math.sin(timeMs / 165.0 + i * 2) * 4);
                y += (int) Math.round(Math.cos(timeMs / 185.0 + i * 2) * 5);
                rect(canvas, x - 1, y, 3, 2, 0xFF373428);
                rect(canvas, x - 2, y - (timeMs % 140 < 70 ? 2 : 1), 2, 2, 0xFFC6BD98);
                rect(canvas, x + 1, y - (timeMs % 140 < 70 ? 1 : 2), 2, 2, 0xFFD3CAAA);
            }
        }
    }

    private void spots(Canvas canvas, Shape shape, int count, int seed, int color, int shade) {
        for (int i = 0; i < count; i++) {
            int y = 5 + hash(i, seed) % (shape.height - 11);
            int x = skinX(shape, y, i, seed + 1);
            int size = 1 + hash(i, seed + 2) % 3;
            tile(canvas, shape, x, y, size + 1, size, color);
            tile(canvas, shape, x - 1, y + 1, size, 1, shade);
        }
    }

    private void hairs(Canvas canvas, Shape shape, int fuzz) {
        int count = 10 + fuzz / 5;
        for (int i = 0; i < count; i++) {
            int y = 4 + hash(i, 100) % (shape.height - 8);
            int direction = i % 2 == 0 ? 1 : -1;
            int x = direction == 1 ? shape.right[y] + 1 : shape.left[y] - 1;
            int color = i % 3 == 0 ? 0xFF827458 : 0xFF4B4439;
            rect(canvas, x, y - 2, 1, 3, color);
            rect(canvas, x + direction, y - 3, 1, 2, color);
            if (fuzz >= 70) rect(canvas, x + 2 * direction, y - 4, 1, 2, color);
            if (i % 2 == 0) {
                int inside = skinX(shape, y, i, 112);
                tile(canvas, shape, inside, y, 1, 2, color);
                tile(canvas, shape, inside + 1, y - 1, 1, 1, color);
            }
        }
    }

    private void particles(Canvas canvas, Shape shape, int action, float progress) {
        int base = action == 0 ? 0xFF739DA0 : action == 1 ? 0xFFC39453 : action == 2 ? 0xFFF0E2A9 : 0xFF615340;
        int alpha = Math.min(255, (int) (400 * (1 - progress)));
        int color = base & 0x00FFFFFF | alpha << 24;
        int spread = shape.width / 2 + 8;

        for (int i = 0; i < 8; i++) {
            int x = hash(i, 62) % (spread * 2 + 1) - spread;
            int y = hash(i, 61) % 25 - 16;
            if (action == 0) {
                y += Math.round(-27 + progress * 52);
                drop(canvas, x, y, color, color);
            } else if (action == 1) {
                y -= Math.round(progress * 22);
                x += Math.round((i % 2 == 0 ? 1 : -1) * progress * 12);
                rect(canvas, x, y, 2, 2, color);
                if (i % 3 == 0) rect(canvas, x + 1, y - 1, 2, 1, color);
            } else if (action == 2) {
                if (i % 2 == 0) sparkle(canvas, x, y, 1 + Math.round(progress * 5), color);
            } else {
                y += Math.round(progress * progress * 42);
                x += Math.round((i % 2 == 0 ? 1 : -1) * progress * 11);
                rect(canvas, x, y, 1, 3, color);
                rect(canvas, x + 1, y + 2, 1, 2, color);
            }
        }
    }

    private void drop(Canvas canvas, int x, int y, int color, int light) {
        rect(canvas, x, y, 1, 2, color);
        rect(canvas, x - 1, y + 2, 3, 2, color);
        rect(canvas, x, y + 2, 1, 1, light);
        rect(canvas, x, y + 4, 1, 1, color);
    }

    private void sparkle(Canvas canvas, int x, int y, int size, int color) {
        rect(canvas, x - size, y, size * 2 + 1, 1, color);
        rect(canvas, x, y - size, 1, size * 2 + 1, color);
        rect(canvas, x - 1, y - 1, 3, 3, color);
    }

    private static int skinX(Shape shape, int y, int index, int seed) {
        int left = shape.left[y] + 2;
        return left + hash(index, seed) % Math.max(1, shape.right[y] - left - 1);
    }

    private void tile(Canvas canvas, Shape shape, int x, int y, int width, int height, int color) {
        for (int row = Math.max(1, y); row < Math.min(shape.height - 1, y + height); row++) {
            int left = Math.max(x, Math.max(shape.left[row], Math.max(shape.left[row - 1], shape.left[row + 1])));
            int right = Math.min(x + width - 1, Math.min(shape.right[row], Math.min(shape.right[row - 1], shape.right[row + 1])));
            if (left <= right) rect(canvas, left, row, right - left + 1, 1, color);
        }
    }

    private void rect(Canvas canvas, int x, int y, int width, int height, int color) {
        paint.setColor(color);
        canvas.drawRect(x, y, x + width, y + height, paint);
    }

    private static int hash(int index, int seed) {
        int mixed = (index + 31) * 0x45D9F3B ^ (seed + 12) * 0x119DE1F3;
        mixed = ((mixed >>> 16) ^ mixed) * 0x45D9F3B;
        return ((mixed >>> 16) ^ mixed) & Integer.MAX_VALUE;
    }

    private static int clamp(int value, int low, int high) {
        return Math.min(high, Math.max(low, value));
    }

    private static final class Shape {
        final int kind;
        final int height;
        final int width;
        final int[] left;
        final int[] right;
        final int[] center;

        Shape(int kind, int height, int width, int warp) {
            this.kind = kind;
            this.height = height;
            this.width = width;
            left = new int[height];
            right = new int[height];
            center = new int[height];

            for (int y = 0; y < height; y++) {
                double t = y / (height - 1.0);
                double cap = Math.min(1, Math.min(t / .21, (1 - t) / .18));
                double rounded = Math.sqrt(1 - (1 - cap) * (1 - cap));
                double span;
                double bend = Math.sin(t * 5.6) * warp / 11.0;
                switch (kind) {
                    case 0:
                        span = (.56 + .47 * Math.sin(t * Math.PI / 2)) * (.2 + .8 * rounded);
                        bend -= (1 - t) * 4;
                        break;
                    case 1:
                        span = (.88 + .12 * Math.sin(t * Math.PI)) * (.30 + .70 * rounded);
                        bend += Math.sin(t * 3.9) * 2.2;
                        break;
                    case 2:
                        span = (.68 + .32 * t) * (.34 + .66 * rounded);
                        bend -= Math.sin(t * 3.6) * 1.6;
                        break;
                    case 3:
                        span = Math.max(.07, (1 - t) * (.65 + .39 * Math.min(1, t / .15)));
                        bend += t * t * 4;
                        break;
                    default:
                        span = (.47 + .53 * Math.sin(t * Math.PI)) * (.56 + .44 * rounded);
                        bend = Math.sin(t * Math.PI) * (16 + warp * .15) - 9 + 5 * t;
                        break;
                }
                center[y] = (int) Math.round(bend);
                int half = Math.max(1, (int) Math.round(width * span / 2));
                left[y] = center[y] - half;
                right[y] = center[y] + half;
            }
        }
    }
}
