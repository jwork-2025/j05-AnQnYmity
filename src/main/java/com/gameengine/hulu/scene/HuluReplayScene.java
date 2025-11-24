package com.gameengine.hulu.scene;

import com.gameengine.components.TransformComponent;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameObject;
import com.gameengine.graphics.IRenderer;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;
import com.gameengine.recording.FileRecordingStorage;
import com.gameengine.recording.RecordingJson;
import com.gameengine.recording.RecordingStorage;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HuluReplayScene extends Scene {
    private final GameEngine engine;
    private String recordingPath;
    private IRenderer renderer;
    private InputManager input;
    private float time;

    private static class Keyframe {
        static class EntityInfo {
            Vector2 pos;
            String rt; // RECTANGLE/CIRCLE/LINE/CUSTOM/null
            float w, h;
            float r=0.9f,g=0.9f,b=0.2f,a=1.0f; // 默认颜色
            String id;
        }
        double t;
        List<EntityInfo> entities = new ArrayList<>();
    }

    private final List<Keyframe> keyframes = new ArrayList<>();
    private final List<GameObject> objectList = new ArrayList<>();

    // 如果 path 为 null，则先展示 recordings 目录下的文件列表，供用户选择
    public HuluReplayScene(GameEngine engine, String path) {
        super("HuluReplay");
        this.engine = engine;
        this.recordingPath = path;
    }

    @Override
    public void initialize() {
        super.initialize();
        this.renderer = engine.getRenderer();
        this.input = engine.getInputManager();
        // 重置状态
        this.time = 0f;
        this.keyframes.clear();
        this.objectList.clear();
        if (recordingPath != null) {
            loadRecording(recordingPath);
            buildObjectsFromFirstKeyframe();
        } else {
            // 仅进入文件选择模式
            this.recordingFiles = null;
            this.selectedIndex = 0;
        }
    }

    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        // ESC (27) or P (80) to return to menu
        if (input.isKeyJustPressed(27) || input.isKeyJustPressed(80)) {
            // Return to menu
            engine.setScene(new HuluMenuScene(engine));
            return;
        }
        // 文件选择模式
        if (recordingPath == null) {
            handleFileSelection();
            return;
        }

        if (keyframes.size() < 1) return;
        time += deltaTime;
        // 限制在最后关键帧处停止
        double lastT = keyframes.get(keyframes.size() - 1).t;
        if (time > lastT) {
            time = (float)lastT;
        }

        // 查找区间
        Keyframe a = keyframes.get(0);
        Keyframe b = keyframes.get(keyframes.size() - 1);
        for (int i = 0; i < keyframes.size() - 1; i++) {
            Keyframe k1 = keyframes.get(i);
            Keyframe k2 = keyframes.get(i + 1);
            if (time >= k1.t && time <= k2.t) { a = k1; b = k2; break; }
        }
        double span = Math.max(1e-6, b.t - a.t);
        double u = Math.min(1.0, Math.max(0.0, (time - a.t) / span));

        updateInterpolatedPositions(a, b, (float)u);
    }

    @Override
    public void render() {
        renderer.drawRect(0, 0, renderer.getWidth(), renderer.getHeight(), 0.06f, 0.06f, 0.08f, 1.0f);
        if (recordingPath == null) {
            renderFileList();
            return;
        }
        // 基于 Transform 手动绘制
        super.render();
        String hint = "REPLAY: ESC to exit";
        float w = hint.length() * 12.0f;
        renderer.drawText(renderer.getWidth()/2.0f - w/2.0f, 30, hint, 0.8f, 0.8f, 0.8f, 1.0f);
    }

    private void loadRecording(String path) {
        keyframes.clear();
        RecordingStorage storage = new FileRecordingStorage();
        try {
            for (String line : storage.readLines(path)) {
                if (line.contains("\"type\":\"keyframe\"")) {
                    Keyframe kf = new Keyframe();
                    kf.t = RecordingJson.parseDouble(RecordingJson.field(line, "t"));
                    int idx = line.indexOf("\"entities\":[");
                    if (idx >= 0) {
                        int bracket = line.indexOf('[', idx);
                        String arr = bracket >= 0 ? RecordingJson.extractArray(line, bracket) : "";
                        String[] parts = RecordingJson.splitTopLevel(arr);
                        for (String p : parts) {
                            Keyframe.EntityInfo ei = new Keyframe.EntityInfo();
                            ei.id = RecordingJson.stripQuotes(RecordingJson.field(p, "id"));
                            double x = RecordingJson.parseDouble(RecordingJson.field(p, "x"));
                            double y = RecordingJson.parseDouble(RecordingJson.field(p, "y"));
                            ei.pos = new Vector2((float)x, (float)y);
                            String rt = RecordingJson.stripQuotes(RecordingJson.field(p, "rt"));
                            ei.rt = rt;
                            ei.w = (float)RecordingJson.parseDouble(RecordingJson.field(p, "w"));
                            ei.h = (float)RecordingJson.parseDouble(RecordingJson.field(p, "h"));
                            String colorArr = RecordingJson.field(p, "color");
                            if (colorArr != null && colorArr.startsWith("[")) {
                                String c = colorArr.substring(1, Math.max(1, colorArr.indexOf(']', 1)));
                                String[] cs = c.split(",");
                                if (cs.length >= 3) {
                                    try {
                                        ei.r = Float.parseFloat(cs[0].trim());
                                        ei.g = Float.parseFloat(cs[1].trim());
                                        ei.b = Float.parseFloat(cs[2].trim());
                                        if (cs.length >= 4) ei.a = Float.parseFloat(cs[3].trim());
                                    } catch (Exception ignored) {}
                                }
                            }
                            kf.entities.add(ei);
                        }
                    }
                    keyframes.add(kf);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        keyframes.sort(Comparator.comparingDouble(k -> k.t));
    }

    private void buildObjectsFromFirstKeyframe() {
        if (keyframes.isEmpty()) return;
        Keyframe kf0 = keyframes.get(0);
        objectList.clear();
        clear();
        for (int i = 0; i < kf0.entities.size(); i++) {
            GameObject obj = buildObjectFromEntity(kf0.entities.get(i), i);
            addGameObject(obj);
            objectList.add(obj);
        }
        time = 0f;
    }

    private void ensureObjectCount(int n) {
        while (objectList.size() < n) {
            GameObject obj = new GameObject("RObj#" + objectList.size());
            obj.addComponent(new TransformComponent(new Vector2(0, 0)));
            addGameObject(obj);
            objectList.add(obj);
        }
        while (objectList.size() > n) {
            GameObject obj = objectList.remove(objectList.size() - 1);
            obj.setActive(false);
        }
    }

    private void updateInterpolatedPositions(Keyframe a, Keyframe b, float u) {
        int n = Math.min(a.entities.size(), b.entities.size());
        ensureObjectCount(n);
        for (int i = 0; i < n; i++) {
            Vector2 pa = a.entities.get(i).pos;
            Vector2 pb = b.entities.get(i).pos;
            float x = (float)((1.0 - u) * pa.x + u * pb.x);
            float y = (float)((1.0 - u) * pa.y + u * pb.y);
            GameObject obj = objectList.get(i);
            TransformComponent tc = obj.getComponent(TransformComponent.class);
            if (tc != null) tc.setPosition(new Vector2(x, y));
        }
    }

    private GameObject buildObjectFromEntity(Keyframe.EntityInfo ei, int index) {
        GameObject obj = new GameObject(ei.id == null ? ("Obj#"+index) : ei.id);
        obj.addComponent(new TransformComponent(new Vector2(ei.pos)));
        
        // 通用渲染组件重建
        if ("CIRCLE".equals(ei.rt)) {
            com.gameengine.components.RenderComponent rc = obj.addComponent(
                new com.gameengine.components.RenderComponent(
                    com.gameengine.components.RenderComponent.RenderType.CIRCLE,
                    new Vector2(Math.max(1, ei.w), Math.max(1, ei.h)),
                    new com.gameengine.components.RenderComponent.Color(ei.r, ei.g, ei.b, ei.a)
                )
            );
            rc.setRenderer(renderer);
        } else if ("RECTANGLE".equals(ei.rt)) {
            com.gameengine.components.RenderComponent rc = obj.addComponent(
                new com.gameengine.components.RenderComponent(
                    com.gameengine.components.RenderComponent.RenderType.RECTANGLE,
                    new Vector2(Math.max(1, ei.w), Math.max(1, ei.h)),
                    new com.gameengine.components.RenderComponent.Color(ei.r, ei.g, ei.b, ei.a)
                )
            );
            rc.setRenderer(renderer);
        } else {
            // 默认矩形
            com.gameengine.components.RenderComponent rc = obj.addComponent(
                new com.gameengine.components.RenderComponent(
                    com.gameengine.components.RenderComponent.RenderType.RECTANGLE,
                    new Vector2(Math.max(1, ei.w > 0 ? ei.w : 20), Math.max(1, ei.h > 0 ? ei.h : 20)),
                    new com.gameengine.components.RenderComponent.Color(ei.r, ei.g, ei.b, ei.a)
                )
            );
            rc.setRenderer(renderer);
        }
        
        return obj;
    }

    // ========== 文件列表模式 ==========
    private List<File> recordingFiles;
    private int selectedIndex = 0;

    private void ensureFilesListed() {
        if (recordingFiles != null) return;
        RecordingStorage storage = new FileRecordingStorage();
        recordingFiles = storage.listRecordings();
    }

    private void handleFileSelection() {
        ensureFilesListed();
        if (input.isKeyJustPressed(38) || input.isKeyJustPressed(265)) { // up
            selectedIndex = (selectedIndex - 1 + Math.max(1, recordingFiles.size())) % Math.max(1, recordingFiles.size());
        } else if (input.isKeyJustPressed(40) || input.isKeyJustPressed(264)) { // down
            selectedIndex = (selectedIndex + 1) % Math.max(1, recordingFiles.size());
        } else if (input.isKeyJustPressed(10) || input.isKeyJustPressed(32) || input.isKeyJustPressed(257)) { // enter/space
            if (recordingFiles.size() > 0) {
                String path = recordingFiles.get(selectedIndex).getAbsolutePath();
                this.recordingPath = path;
                clear();
                initialize();
            }
        }
    }

    private void renderFileList() {
        ensureFilesListed();
        int w = renderer.getWidth();
        int h = renderer.getHeight();
        String title = "SELECT HULU RECORDING";
        float tw = title.length() * 16f;
        renderer.drawText(w/2f - tw/2f, 80, title, 1f,1f,1f,1f);

        if (recordingFiles.isEmpty()) {
            String none = "NO RECORDINGS FOUND";
            float nw = none.length() * 14f;
            renderer.drawText(w/2f - nw/2f, h/2f, none, 0.9f,0.8f,0.2f,1f);
            return;
        }

        float startY = 140f;
        float itemH = 28f;
        for (int i = 0; i < recordingFiles.size(); i++) {
            String name = recordingFiles.get(i).getName();
            float x = 100f;
            float y = startY + i * itemH;
            if (i == selectedIndex) {
                renderer.drawRect(x - 10, y - 6, 600, 24, 0.3f,0.3f,0.4f,0.8f);
            }
            renderer.drawText(x, y, name, 0.9f,0.9f,0.9f,1f);
        }
    }
}
