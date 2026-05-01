#include <jni.h>
#include <string>
#include <vector>
#include <cmath>
#include <random>
#include <algorithm>
#include <ctime>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

const float MAX_RADIUS = 170.0f;
const float START_RADIUS = 30.0f;
const float MAP_RADIUS = 8500.0f;
const float MAP_CENTER = 8500.0f;

struct Player {
    float x, y, radius, angle, speed;
};

struct Web {
    float x, y, radius;
};

struct NPC {
    float x, y, radius, speed, angle;
    int turnTimer;
};

class Game {
public:
    Player player;
    std::vector<Web> webs;
    std::vector<NPC> npcs;
    int score = 0;
    bool gameActive = false;
    float joyX = 0, joyY = 0;

    Game() {
        srand(time(NULL));
        reset();
    }

    bool isInsideOctagon(float px, float py, float margin = 0) {
        float r = MAP_RADIUS - margin;
        for (int i = 0; i < 8; i++) {
            float angle1 = (M_PI / 4.0) * i;
            float angle2 = (M_PI / 4.0) * (i + 1);
            float x1 = MAP_CENTER + r * cos(angle1);
            float y1 = MAP_CENTER + r * sin(angle1);
            float x2 = MAP_CENTER + r * cos(angle2);
            float y2 = MAP_CENTER + r * sin(angle2);
            if ((x2 - x1) * (py - y1) - (y2 - y1) * (px - x1) < 0) return false;
        }
        return true;
    }

    void spawnWeb() {
        float x, y;
        do {
            x = MAP_CENTER + (float(rand()) / RAND_MAX - 0.5f) * MAP_RADIUS * 2.1f;
            y = MAP_CENTER + (float(rand()) / RAND_MAX - 0.5f) * MAP_RADIUS * 2.1f;
        } while (!isInsideOctagon(x, y, 1000.0f));
        webs.push_back({x, y, 22.0f});
    }

    void spawnNPC(bool init = false) {
        float x, y;
        do {
            x = MAP_CENTER + (float(rand()) / RAND_MAX - 0.5f) * MAP_RADIUS * 2.1f;
            y = MAP_CENTER + (float(rand()) / RAND_MAX - 0.5f) * MAP_RADIUS * 2.1f;
        } while (!isInsideOctagon(x, y, 100.0f));
        npcs.push_back({
            x, y,
            init ? (30.0f + (float(rand()) / RAND_MAX) * 45.0f) : 30.0f,
            2.0f + (float(rand()) / RAND_MAX) * 2.2f,
            (float(rand()) / RAND_MAX) * (float)M_PI * 2.0f,
            0
        });
    }

    void reset() {
        player = {MAP_CENTER, MAP_CENTER, START_RADIUS, 0, 10.0f};
        webs.clear();
        npcs.clear();
        score = 0;
        for (int i = 0; i < 400; i++) spawnWeb();
        for (int i = 0; i < 60; i++) spawnNPC(true);
        gameActive = false;
    }

    void update(float dt) {
        if (!gameActive) return;

        float dx = joyX;
        float dy = joyY;
        // Adjust speed by dt (assuming dt is in seconds, 60fps = 0.016s)
        // Original speed was per-frame at 60fps.
        float speedMultiplier = dt / 0.0166f;
        float pSpd = player.speed * std::max(0.4f, 1.0f - (player.radius / (MAX_RADIUS * 1.5f))) * speedMultiplier;

        if (std::hypot(dx, dy) > 0.01f) {
            player.angle = std::atan2(dy, dx);
            float nx = player.x + std::cos(player.angle) * pSpd;
            float ny = player.y + std::sin(player.angle) * pSpd;
            if (isInsideOctagon(nx, ny, player.radius)) {
                player.x = nx;
                player.y = ny;
            }
        }

        for (size_t i = 0; i < npcs.size(); ++i) {
            NPC &n = npcs[i];
            n.turnTimer -= (int)(speedMultiplier);
            if (n.turnTimer <= 0) {
                n.angle += (float(rand()) / RAND_MAX - 0.5f) * 2.0f;
                n.turnTimer = 40 + rand() % 60;
            }
            float nSpd = n.speed * std::max(0.4f, 1.0f - (n.radius / (MAX_RADIUS * 1.5f))) * speedMultiplier;
            float nx = n.x + std::cos(n.angle) * nSpd;
            float ny = n.y + std::sin(n.angle) * nSpd;

            if (isInsideOctagon(nx, ny, n.radius)) {
                n.x = nx;
                n.y = ny;
            } else {
                n.angle += M_PI * 0.8f;
            }

            float d = std::hypot(player.x - n.x, player.y - n.y);
            if (d < player.radius + n.radius - 12.0f) {
                if (player.radius > n.radius) {
                    player.radius = std::min(MAX_RADIUS, player.radius + n.radius * 0.1f);
                    score += 10;
                    npcs.erase(npcs.begin() + i);
                    spawnNPC();
                    --i;
                } else {
                    gameActive = false;
                }
            }
        }

        for (size_t i = 0; i < webs.size(); ++i) {
            Web &w = webs[i];
            if (std::hypot(player.x - w.x, player.y - w.y) < player.radius) {
                score += 1;
                player.radius = std::min(MAX_RADIUS, player.radius + 1.25f);
                webs.erase(webs.begin() + i);
                spawnWeb();
                --i;
            }
        }
    }
};

static Game g_game;

extern "C" JNIEXPORT void JNICALL
Java_com_silkweb_silkweb_1spiderkingdom_12d_MainActivity_initGame(JNIEnv *env, jobject thiz) {
    g_game.reset();
}

extern "C" JNIEXPORT void JNICALL
Java_com_silkweb_silkweb_1spiderkingdom_12d_MainActivity_startGame(JNIEnv *env, jobject thiz) {
    g_game.gameActive = true;
}

extern "C" JNIEXPORT void JNICALL
Java_com_silkweb_silkweb_1spiderkingdom_12d_MainActivity_updateGame(JNIEnv *env, jobject thiz, jfloat dt) {
    g_game.update(dt);
}

extern "C" JNIEXPORT void JNICALL
Java_com_silkweb_silkweb_1spiderkingdom_12d_MainActivity_setJoystick(JNIEnv *env, jobject thiz, jfloat x, jfloat y) {
    g_game.joyX = x;
    g_game.joyY = y;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_silkweb_silkweb_1spiderkingdom_12d_MainActivity_getPlayerState(JNIEnv *env, jobject thiz) {
    float state[6] = {g_game.player.x, g_game.player.y, g_game.player.radius, g_game.player.angle, (float)g_game.score, g_game.gameActive ? 1.0f : 0.0f};
    jfloatArray result = env->NewFloatArray(6);
    env->SetFloatArrayRegion(result, 0, 6, state);
    return result;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_silkweb_silkweb_1spiderkingdom_12d_MainActivity_getWebs(JNIEnv *env, jobject thiz) {
    size_t size = g_game.webs.size();
    jfloatArray result = env->NewFloatArray(size * 3);
    if (size > 0) {
        float *temp = new float[size * 3];
        for (size_t i = 0; i < size; ++i) {
            temp[i * 3] = g_game.webs[i].x;
            temp[i * 3 + 1] = g_game.webs[i].y;
            temp[i * 3 + 2] = g_game.webs[i].radius;
        }
        env->SetFloatArrayRegion(result, 0, size * 3, temp);
        delete[] temp;
    }
    return result;
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_silkweb_silkweb_1spiderkingdom_12d_MainActivity_getNPCs(JNIEnv *env, jobject thiz) {
    size_t size = g_game.npcs.size();
    jfloatArray result = env->NewFloatArray(size * 4);
    if (size > 0) {
        float *temp = new float[size * 4];
        for (size_t i = 0; i < size; ++i) {
            temp[i * 4] = g_game.npcs[i].x;
            temp[i * 4 + 1] = g_game.npcs[i].y;
            temp[i * 4 + 2] = g_game.npcs[i].radius;
            temp[i * 4 + 3] = g_game.npcs[i].angle;
        }
        env->SetFloatArrayRegion(result, 0, size * 4, temp);
        delete[] temp;
    }
    return result;
}
