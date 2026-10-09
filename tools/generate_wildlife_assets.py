"""Original, deterministic voxel Jaguar source/exporter. No third-party art or recordings.

Run with Python 3.12+, Pillow 11.1.0, numpy 2.2.6 and soundfile 0.13.1.
Generated files are committed; normal Gradle builds do not require Python.
"""
from pathlib import Path
import json
import math
import random
import shutil
import numpy as np
import soundfile as sf
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "assets/java/pack"
BEDROCK = ROOT / "assets/bedrock/pack"
NS = "ggwpwildlands"


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


# Each centered cuboid uses Minecraft's 16 model units per block.
BONES = {
    "body": ([0, 8.8, 0], [8, 7, 18]),
    "head": ([0, 10.4, -10.4], [7, 6, 7]),
    "front_left": ([3.52, 3.2, -6.4], [2.5, 6.4, 2.5]),
    "front_right": ([-3.52, 3.2, -6.4], [2.5, 6.4, 2.5]),
    "rear_left": ([3.52, 3.2, 6.4], [2.5, 6.4, 2.5]),
    "rear_right": ([-3.52, 3.2, 6.4], [2.5, 6.4, 2.5]),
    "tail": ([0, 8.8, 14], [2, 2, 10]),
}


def export():
    write(JAVA / "pack.mcmeta", {"pack": {"min_format": [88, 0], "max_format": [88, 0], "description": "GGWP Wildlands — original Jaguar prototype"}})
    write(BEDROCK / "manifest.json", {"format_version": 2, "header": {
        "name": "GGWP Wildlands Jaguar Prototype", "description": "Original Jaguar presentation; gameplay stays on Paper",
        "uuid": "abe98b0d-4cf4-423b-9b0f-2029a4732301", "version": [0, 10, 1], "min_engine_version": [1, 26, 30]},
        "modules": [{"type": "resources", "uuid": "616b98cf-36db-49a4-8edc-b7e11c2f3301", "version": [0, 10, 1]}]})
    randomizer = random.Random(4509)
    texture = Image.new("RGB", (64, 64), (202, 147, 57))
    draw = ImageDraw.Draw(texture)
    for y in range(64):
        for x in range(64):
            shade = randomizer.randrange(-12, 13)
            texture.putpixel((x, y), (202 + shade, 147 + shade, 57 + shade))
    for y in range(3, 64, 10):
        for x in range(3, 64, 10):
            x += randomizer.randrange(-2, 3)
            draw.ellipse((x, y, x+6, y+6), outline=(49, 33, 23), width=2)
            draw.point((x+3, y+3), fill=(76, 48, 25))
    texture_path = JAVA / f"assets/{NS}/textures/entity/jaguar.png"
    texture_path.parent.mkdir(parents=True, exist_ok=True)
    texture.save(texture_path)
    (BEDROCK / "textures/entity").mkdir(parents=True, exist_ok=True)
    shutil.copyfile(texture_path, BEDROCK / "textures/entity/jaguar.png")
    # Solid face accents occupy their own stable pixels in the atlas.
    texture.putpixel((63, 63), (18, 16, 12))
    texture.putpixel((62, 63), (220, 196, 115))
    texture.save(texture_path)
    shutil.copyfile(texture_path, BEDROCK / "textures/entity/jaguar.png")
    geometry_bones = []
    for name, (pivot, size) in BONES.items():
        # Java item models are centered on (8,8,8) by the ItemDisplay renderer.
        low = [8 - value/2 for value in size]
        high = [8 + value/2 for value in size]
        faces = {direction: {"uv": [0, 0, 16, 16], "texture": "#fur"} for direction in ["north", "south", "east", "west", "up", "down"]}
        elements = [{"from": low, "to": high, "faces": faces}]
        if name == "head":
            for x in [4.7, 10.5]:
                elements.append({"from": [x, 9, 4.4], "to": [x+.8, 9.8, 4.6], "faces": {
                    direction: {"uv": [15.75, 15.75, 16, 16], "texture": "#fur"} for direction in faces}})
            for x in [4.6, 9.4]:
                elements.append({"from": [x, 11, 7], "to": [x+2, 13, 9], "faces": faces})
        write(JAVA / f"assets/{NS}/models/jaguar/{name}.json", {"textures": {"fur": f"{NS}:entity/jaguar"}, "elements": elements})
        write(JAVA / f"assets/{NS}/items/jaguar/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:jaguar/{name}"}})
        # Both native feline model conventions place the head toward negative Z.
        bedrock_pivot = list(pivot)
        origin = [bedrock_pivot[index] - size[index]/2 for index in range(3)]
        cubes = [{"origin": origin, "size": size, "uv": [0, 0]}]
        if name == "head":
            for x in [-3.3, 2.5]:
                cubes.append({"origin": [x, 11.4, -14.05], "size": [.8, .8, .2], "uv": {face: {"uv": [63, 63], "uv_size": [1, 1]} for face in faces}})
            for x in [-3.4, 1.4]:
                cubes.append({"origin": [x, 13.4, -11.4], "size": [2, 2, 2], "uv": [0, 0]})
        geometry_bones.append({"name": name, "pivot": bedrock_pivot, "cubes": cubes})
    write(BEDROCK / "models/entity/jaguar.geo.json", {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": f"geometry.{NS}.jaguar", "texture_width": 64, "texture_height": 64,
                        "visible_bounds_width": 3, "visible_bounds_height": 2, "visible_bounds_offset": [0, 1, 0]}, "bones": geometry_bones}]})
    animations = {
        f"animation.{NS}.jaguar.idle": {"loop": True, "bones": {"tail": {"rotation": [0, "math.sin(query.life_time*90)*5", 0]}}},
        f"animation.{NS}.jaguar.walk": {"loop": True, "bones": {name: {"rotation": [f"math.sin(query.modified_distance_moved*180+{0 if i in [0,3] else 180})*24", 0, 0]} for i, name in enumerate(list(BONES)[2:6])}},
        f"animation.{NS}.jaguar.stalk": {"loop": True, "bones": {name: {"position": [0, -1.28, 0]} for name in BONES}},
        f"animation.{NS}.jaguar.warning": {"loop": True, "bones": {"head": {"rotation": [-12, 0, 0]}, "tail": {"rotation": [0, "math.sin(query.life_time*180)*18", 0]}}},
        f"animation.{NS}.jaguar.attack": {"loop": True, "bones": {"head": {"rotation": ["math.sin(query.life_time*540)*8", 0, 0]}}},
        f"animation.{NS}.jaguar.death": {"animation_length": 1, "bones": {name: {"rotation": [0, 0, 75]} for name in BONES}},
    }
    write(BEDROCK / "animations/jaguar.animation.json", {"format_version": "1.8.0", "animations": animations})
    description = {"identifier": f"{NS}:jaguar", "materials": {"default": "entity_alphatest"}, "textures": {"default": "textures/entity/jaguar"},
        "geometry": {"default": f"geometry.{NS}.jaguar"}, "animations": {key: f"animation.{NS}.jaguar.{key}" for key in ["idle", "walk", "stalk", "warning", "attack", "death"]},
        "scripts": {"animate": ["idle", {"walk": "query.modified_move_speed > 0.01"}, {"stalk": f"query.property('{NS}:phase') == 1"},
                                {"warning": f"query.property('{NS}:phase') == 2"}, {"attack": f"query.property('{NS}:phase') == 3"}, {"death": "!query.is_alive"}]},
        "render_controllers": ["controller.render.ggwpwildlands.jaguar"]}
    write(BEDROCK / "entity/jaguar.entity.json", {"format_version": "1.10.0", "minecraft:client_entity": {"description": description}})
    write(BEDROCK / "render_controllers/jaguar.render_controllers.json", {"format_version": "1.8.0", "render_controllers": {
        "controller.render.ggwpwildlands.jaguar": {"geometry": "Geometry.default", "materials": [{"*": "Material.default"}], "textures": ["Texture.default"]}}})
    java_sounds, bedrock_sounds = {}, {}
    for cue, frequency, duration in [("idle", 130, .4), ("warning", 72, .8), ("attack", 85, .45), ("hurt", 180, .4), ("death", 65, .9)]:
        sample_rate = 22050
        t = np.arange(int(sample_rate * duration)) / sample_rate
        rng = np.random.default_rng(4509 + int(frequency))
        noise = np.convolve(rng.uniform(-1, 1, len(t)), np.ones(30)/30, mode="same")
        wave = (np.sin(2*math.pi*frequency*t + 4*np.sin(2*math.pi*19*t))*.22 + noise*.6) * np.sin(math.pi*t/duration)**2
        java_path = JAVA / f"assets/{NS}/sounds/jaguar/{cue}.ogg"
        java_path.parent.mkdir(parents=True, exist_ok=True)
        sf.write(java_path, wave, sample_rate, format="OGG", subtype="VORBIS")
        bedrock_path = BEDROCK / f"sounds/jaguar/{cue}.ogg"
        bedrock_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(java_path, bedrock_path)
        java_sounds[f"{NS}:jaguar.{cue}"] = {"sounds": [{"name": f"{NS}:jaguar/{cue}", "stream": False}]}
        bedrock_sounds[f"{NS}:jaguar.{cue}"] = {"category": "neutral", "sounds": [f"sounds/jaguar/{cue}"]}
    # Java's event namespace is already ggwpwildlands, so the key uses jaguar.*.
    write(JAVA / f"assets/{NS}/sounds.json", {key.removeprefix(NS + ":"): value for key, value in java_sounds.items()})
    write(BEDROCK / "sounds/sound_definitions.json", {"format_version": "1.14.0", "sound_definitions": bedrock_sounds})


if __name__ == "__main__":
    export()
