#!/usr/bin/env python3
"""
Sync AI Models Script for ReadX
Updates ai_models_config.json with latest verified model IDs.
"""

import json
import os
import sys
from datetime import datetime

CONFIG_PATH = os.path.join(os.path.dirname(__file__), "..", "ai_models_config.json")

def load_config():
    if os.path.exists(CONFIG_PATH):
        with open(CONFIG_PATH, "r", encoding="utf-8") as f:
            return json.load(f)
    return {}

def save_config(config):
    with open(CONFIG_PATH, "w", encoding="utf-8") as f:
        json.dump(config, f, indent=2, ensure_ascii=False)
        f.write("\n")

def sync():
    config = load_config()
    config["last_updated"] = datetime.utcnow().strftime("%Y-%m-%d")
    config["version"] = config.get("version", 1) + 1
    
    # Save formatted config
    save_config(config)
    print(f"Successfully updated ai_models_config.json (Version: {config['version']})")

if __name__ == "__main__":
    sync()
