"""Original, deterministic voxel Jaguar source/exporter. No third-party art or recordings.

Run with Python 3.12+, Pillow 11.1.0, numpy 2.2.6 and soundfile 0.13.1.
Generated files are committed; normal Gradle builds do not require Python.
"""
from pathlib import Path
import json
import math
import random
import shutil
import zlib
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


def export_passive(species, palette, frequency, profile):
    """Each species has authored dimensions/texture and its own sound synthesis."""
    texture = Image.new("RGB", (64, 64), tuple(palette))
    rng = random.Random(4509 + int(frequency))
    for y in range(64):
        for x in range(64):
            shade = rng.randrange(-10, 11)
            texture.putpixel((x, y), tuple(max(0, min(255, channel+shade)) for channel in palette))
    texture.putpixel((63, 63), (18, 16, 12))
    texture.putpixel((62, 63), (239, 178, 41))
    path = JAVA / f"assets/{NS}/textures/entity/{species}.png"
    texture.save(path)
    shutil.copyfile(path, BEDROCK / f"textures/entity/{species}.png")
    geometry = []
    for name, (pivot, size) in zip(BONES, profile):
        cubes = [([-v/2 for v in size], size, False)]
        if name == "head":
            for x in [-size[0]/2+.2, size[0]/2-1]:
                cubes.append(([x, .8, -size[2]/2-.1], [.8, .8, .2], True))
                if species in ["capybara", "tapir", "monkey"]: cubes.append(([x, size[1]/2-.5, -.5], [1.5, 1.5, 1.5], False))
            if species == "tapir": cubes.append(([-1.25, -2, -size[2]/2-2.5], [2.5, 2.5, 3], False))
            if species == "toucan": cubes.append(([-1.2, -1, -size[2]/2-4], [2.4, 2, 4.5], "beak"))
            if species == "caiman": cubes.append(([-2.5, -1.4, -size[2]/2-3], [5, 2, 4], False))
        elements, bedrock_cubes = [], []
        for origin, dimensions, accent in cubes:
            directions = ["north", "south", "east", "west", "up", "down"]
            faces = {face: {"uv": [15.5, 15.75, 15.75, 16] if accent == "beak" else [15.75, 15.75, 16, 16] if accent else [0, 0, 16, 16], "texture": "#fur"} for face in directions}
            elements.append({"from": [8+v for v in origin], "to": [8+origin[i]+dimensions[i] for i in range(3)], "faces": faces})
            uv = {face: {"uv": [62 if accent == "beak" else 63, 63], "uv_size": [1, 1]} for face in directions} if accent else [0, 0]
            bedrock_cubes.append({"origin": [pivot[i]+origin[i] for i in range(3)], "size": dimensions, "uv": uv})
        write(JAVA / f"assets/{NS}/models/{species}/{name}.json", {"textures": {"fur": f"{NS}:entity/{species}"}, "elements": elements})
        write(JAVA / f"assets/{NS}/items/{species}/{name}.json", {"model": {"type": "minecraft:model", "model": f"{NS}:{species}/{name}"}})
        geometry.append({"name": name, "pivot": pivot, "cubes": bedrock_cubes})
    def clone(relative):
        return json.loads((BEDROCK / relative).read_text(encoding="utf-8").replace("jaguar", species))
    model = clone("models/entity/jaguar.geo.json")
    model["minecraft:geometry"][0]["bones"] = geometry
    write(BEDROCK / f"models/entity/{species}.geo.json", model)
    animations = clone("animations/jaguar.animation.json")
    animations["animations"][f"animation.{NS}.{species}.stalk"]["bones"] = {}
    animations["animations"][f"animation.{NS}.{species}.idle"]["bones"] = {"head": {"rotation": ["math.sin(query.life_time*40)*2", 0, 0]}}
    animations["animations"][f"animation.{NS}.{species}.swim"] = {"loop": True, "bones": {name: {"rotation": ["math.sin(query.life_time*320)*12", 0, 0]} for name in list(BONES)[2:6]}}
    if species == "toucan":
        animations["animations"][f"animation.{NS}.{species}.walk"]["bones"] = {
            "front_left": {"rotation": [0, 0, "math.sin(query.life_time*520)*35"]},
            "front_right": {"rotation": [0, 0, "-math.sin(query.life_time*520)*35"]}}
    if species == "frog":
        animations["animations"][f"animation.{NS}.{species}.walk"]["bones"] = {name: {"rotation": ["math.sin(query.life_time*320)*20", 0, 0]} for name in list(BONES)[2:6]}
    write(BEDROCK / f"animations/{species}.animation.json", animations)
    entity = clone("entity/jaguar.entity.json")
    definition = entity["minecraft:client_entity"]["description"]
    definition["animations"]["swim"] = f"animation.{NS}.{species}.swim"
    definition["scripts"]["animate"].append({"swim": "query.is_in_water"})
    definition["scripts"]["animate"][1]["walk"] = "query.modified_move_speed > 0.01 && !query.is_in_water"
    write(BEDROCK / f"entity/{species}.entity.json", entity)
    write(BEDROCK / f"render_controllers/{species}.render_controllers.json", clone("render_controllers/jaguar.render_controllers.json"))
    java_sounds = json.loads((JAVA / f"assets/{NS}/sounds.json").read_text())
    bedrock_sounds = json.loads((BEDROCK / "sounds/sound_definitions.json").read_text())
    for cue, multiplier, duration in [("idle", 1, .4), ("warning", .7, .6), ("attack", .8, .45), ("hurt", 1.35, .4), ("death", .5, .9), ("movement", .35, .2)]:
        t = np.arange(int(22050*duration))/22050
        noise = np.convolve(np.random.default_rng(int(frequency*multiplier)+4509).uniform(-1, 1, len(t)), np.ones(30)/30, mode="same")
        wave = (np.sin(2*math.pi*frequency*multiplier*t + 1.5*np.sin(2*math.pi*19*t))*.22 + noise*.15) * np.sin(math.pi*t/duration)**2
        path = JAVA / f"assets/{NS}/sounds/{species}/{cue}.ogg"
        path.parent.mkdir(parents=True, exist_ok=True)
        sf.write(path, wave, 22050, format="OGG", subtype="VORBIS")
        target = BEDROCK / f"sounds/{species}/{cue}.ogg"
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(path, target)
        java_sounds[f"{species}.{cue}"] = {"sounds": [{"name": f"{NS}:{species}/{cue}", "stream": False}]}
        bedrock_sounds["sound_definitions"][f"{NS}:{species}.{cue}"] = {"category": "neutral", "sounds": [f"sounds/{species}/{cue}"]}
    write(JAVA / f"assets/{NS}/sounds.json", java_sounds)
    write(BEDROCK / "sounds/sound_definitions.json", bedrock_sounds)


def canonicalize_ogg(path):
    # libsndfile randomizes Ogg stream serials. Fix serial and per-page Ogg CRC.
    data = bytearray(path.read_bytes())
    serial = zlib.crc32((path.parent.name + "/" + path.name).encode())
    offset = 0
    while offset < len(data):
        assert data[offset:offset+4] == b"OggS"
        segments = data[offset+26]
        length = 27 + segments + sum(data[offset+27:offset+27+segments])
        data[offset+14:offset+18] = serial.to_bytes(4, "little")
        data[offset+22:offset+26] = bytes(4)
        crc = 0
        for value in data[offset:offset+length]:
            crc ^= value << 24
            for _ in range(8): crc = ((crc << 1) ^ (0x04C11DB7 if crc & 0x80000000 else 0)) & 0xFFFFFFFF
        data[offset+22:offset+26] = crc.to_bytes(4, "little")
        offset += length
    # Replace atomically: Windows asset indexers may still hold the just-encoded
    # file open, making an immediate second truncating open fail with EINVAL.
    temporary = path.with_suffix(".ogg.tmp")
    temporary.write_bytes(data)
    temporary.replace(path)


if __name__ == "__main__":
    export()
    export_passive("capybara", [139, 103, 65], 255, [
        ([0, 8, 0], [10, 8, 14]), ([0, 8.8, -8], [8, 6, 7]),
        ([4, 2.24, -4.8], [3, 4.48, 3]), ([-4, 2.24, -4.8], [3, 4.48, 3]),
        ([4, 2.24, 4.8], [3, 4.48, 3]), ([-4, 2.24, 4.8], [3, 4.48, 3]), ([0, 8, 7.52], [.4, .4, .4])])
    export_passive("tapir", [85, 64, 54], 460, [
        ([0, 9.6, 0], [12, 10, 19]), ([0, 9.92, -10.24], [7, 7, 8]),
        ([4.48, 3.2, -5.6], [3.5, 6.4, 3.5]), ([-4.48, 3.2, -5.6], [3.5, 6.4, 3.5]),
        ([4.48, 3.2, 6.4], [3.5, 6.4, 3.5]), ([-4.48, 3.2, 6.4], [3.5, 6.4, 3.5]), ([0, 9.6, 10.24], [1, 1, 2])])
    export_passive("toucan", [29, 33, 39], 750, [
        ([0, 6.4, 0], [5, 6, 7]), ([0, 9.92, -1.92], [4, 4, 4]),
        ([2.88, 6.4, 0], [1, 3.5, 7]), ([-2.88, 6.4, 0], [1, 3.5, 7]),
        ([1.12, 1.6, 0], [1, 3.2, 1]), ([-1.12, 1.6, 0], [1, 3.2, 1]), ([0, 6.4, 4.8], [3, 1, 5])])
    export_passive("monkey", [106, 75, 46], 350, [
        ([0, 7.2, 0], [5, 7, 6]), ([0, 10.4, -3.52], [5, 5, 5]),
        ([2.88, 4, -2.08], [2, 8, 2]), ([-2.88, 4, -2.08], [2, 8, 2]),
        ([2.88, 2.72, 2.56], [2.5, 5.44, 2.5]), ([-2.88, 2.72, 2.56], [2.5, 5.44, 2.5]), ([0, 7.2, 9.6], [1.5, 1.5, 13])])
    export_passive("frog", [69, 143, 47], 180, [
        ([0, 3.2, 0], [5, 3, 5]), ([0, 3.84, -1.92], [5, 3.5, 3]),
        ([2.56, 1.6, -2.56], [1.5, 3.2, 2]), ([-2.56, 1.6, -2.56], [1.5, 3.2, 2]),
        ([3.2, 1.28, 2.4], [3, 2.56, 3]), ([-3.2, 1.28, 2.4], [3, 2.56, 3]), ([0, 3.2, 2.72], [.2, .2, .2])])
    export_passive("caiman", [66, 85, 47], 70, [
        ([0, 3.52, 0], [12, 4, 16]), ([0, 3.84, -8.8], [7, 3, 10]),
        ([5.6, 1.6, -3.84], [4, 3.2, 4]), ([-5.6, 1.6, -3.84], [4, 3.2, 4]),
        ([5.6, 1.6, 4.8], [4, 3.2, 4]), ([-5.6, 1.6, 4.8], [4, 3.2, 4]), ([0, 3.2, 14.4], [4, 3, 14])])
    for root in [JAVA, BEDROCK]:
        for sound in root.rglob("*.ogg"): canonicalize_ogg(sound)
    manifest = json.loads((BEDROCK / "manifest.json").read_text())
    manifest["header"]["name"] = "GGWP Wildlands Wildlife"
    manifest["header"]["description"] = "Seven rainforest species; gameplay stays on Paper"
    manifest["header"]["version"] = manifest["modules"][0]["version"] = [0, 10, 3]
    write(BEDROCK / "manifest.json", manifest)
    metadata = json.loads((JAVA / "pack.mcmeta").read_text())
    metadata["pack"]["description"] = "GGWP Wildlands — seven rainforest species"
    write(JAVA / "pack.mcmeta", metadata)
