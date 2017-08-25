package com.spi.demo.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

public class DemoPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.task('hello') {
            doLast {
                println "Hello World from the DemoPlugin"
            }
        }
    }
}