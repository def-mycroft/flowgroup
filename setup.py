from setuptools import setup, find_packages

setup(
    name="breathing-willow",
    version="0.0.0",
    packages=find_packages(),
    entry_points={
        "console_scripts": [
            "breathing-willow=cli.breathing_willow:main",
        ]
    },
)
