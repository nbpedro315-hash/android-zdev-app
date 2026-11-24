#!/usr/bin/env python3
"""
Simple packer that zips the snake-real artifacts.
"""
import zipfile, os

def main():
    with zipfile.ZipFile('snake-real.zip', 'w') as z:
        for fname in ['Panel.java', 'build.sh', 'inject.py']:
            if os.path.isfile(fname):
                z.write(fname)
    print("Packaged snake-real.zip")
if __name__ == "__main__":
    main()
