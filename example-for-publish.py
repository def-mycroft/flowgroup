# run.py

from breathing_willow import field_publish


if __name__ == '__main__':
    path = '/field/息行 `.flow` Shaping Kernel  characters patient-nebula 2c5c6f02.md'
    print(f"Publishing file: {path}")
    url = field_publish.publish(path)
    print(f"Published to: {url}")

