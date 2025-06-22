from setuptools import setup, find_packages

setup(
    name="breathing-willow",
    version="0.1.0",
    packages=find_packages(),
    entry_points={
        "console_scripts": [
            "breathing-willow=breathing_willow_cli.breathing_willow:main",
        ]
    },
)
