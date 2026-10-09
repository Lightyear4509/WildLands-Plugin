# Original wildlife prototype assets

The Jaguar cuboid model, rosette texture, Java item definitions, Bedrock geometry/animation definitions and synthesized sound cues were authored in this project. No third-party model, texture or animal recording was copied. The synthesis is deliberately stylized; it is not a claim of authentic zoological recording.

Editable source and deterministic exporter: `tools/generate_wildlife_assets.py`. Asset-generation dependencies are listed in `tools/asset-requirements.txt`. Generated assets are committed, so normal Gradle builds require Java 25 only. To regenerate, install those Python dependencies in a local environment and run the script from the repository. The exporter writes both pack source trees separately; it does not convert an arbitrary Java pack.

Pillow, NumPy and SoundFile/libsndfile are build-time asset tools, not runtime plugin dependencies or redistributed copies of third-party media. Their upstream licenses govern those tools. Geyser and Paper API code is compile-only and is not repackaged as Wildlands runtime classes. Project ownership/licensing of the authored assets follows the repository's licensing policy.
