pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven {
            url = uri("https://packages.aliyun.com/6791f66d556e6cdab5370744/maven/repo-izi")
            credentials {
                username = "5ff82b1ca509123753953d60"
                password = "HEz]qf3iHQ9i"
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://packages.aliyun.com/6791f66d556e6cdab5370744/maven/repo-izi")
            credentials {
                username = "5ff82b1ca509123753953d60"
                password = "HEz]qf3iHQ9i"
            }
        }
    }
}

rootProject.name = "CashCredit"
include(":app")
include(":dsbridge")