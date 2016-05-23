/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.plugins.quality

import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.FileCollection
import org.gradle.internal.reflect.Instantiator
import org.gradle.api.internal.project.IsolatedAntBuilder
import org.gradle.api.plugins.quality.internal.JDependReportsImpl
import org.gradle.api.reporting.Reporting
import org.gradle.api.tasks.*

import javax.inject.Inject

/**
 * Analyzes code with <a href="http://clarkware.com/software/JDepend.html">JDepend</a>.
 */
@CompileStatic
class JDepend extends DefaultTask implements Reporting<JDependReports> {
    /**
     * The class path containing the JDepend library to be used.
     */
    @InputFiles
    FileCollection jdependClasspath

    /**
     * The directory containing the classes to be analyzed.
     */
    @InputDirectory
    File classesDir

    // workaround for GRADLE-2026
    /**
     * Returns the directory containing the classes to be analyzed.
     */
    @SkipWhenEmpty
    File getClassesDir() {
        return classesDir
    }

    /**
     * The reports to be generated by this task.
     */
    @Nested
    final JDependReports reports

    JDepend() {
        reports = instantiator.newInstance(JDependReportsImpl, this)
    }

    @Inject
    Instantiator getInstantiator() {
        throw new UnsupportedOperationException();
    }

    @Inject
    IsolatedAntBuilder getAntBuilder() {
        throw new UnsupportedOperationException();
    }

    /**
     * Configures the reports to be generated by this task.
     *
     * The contained reports can be configured by name and closures. Example:
     *
     * <pre>
     * jdependTask {
     *   reports {
     *     xml {
     *       destination "build/jdepend.xml"
     *     }
     *   }
     * }
     * </pre>
     *
     * @param closure The configuration
     * @return The reports container
     */
    JDependReports reports(Closure closure) {
        (JDependReports) reports.configure(closure)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    @TaskAction
    void run() {
        Map<String, Object> reportArguments = [:]
        if (reports.enabled.empty) {
            throw new InvalidUserDataException("JDepend tasks must have one report enabled, however neither the xml or text report are enabled for task '$path'. You need to enable one of them")
        } else if (reports.enabled.size() == 1) {
            reportArguments.outputFile = reports.firstEnabled.destination
            reportArguments.format = reports.firstEnabled.name
        } else {
            throw new InvalidUserDataException("JDepend tasks can only have one report enabled, however both the xml and text report are enabled for task '$path'. You need to disable one of them.")
        }

        antBuilder.withClasspath(getJdependClasspath()).execute {
            ant.taskdef(name: 'jdependreport', classname: 'org.apache.tools.ant.taskdefs.optional.jdepend.JDependTask')
            ant.jdependreport(*:reportArguments, haltonerror: true) {
                classespath {
                    pathElement(location: getClassesDir())
                }
            }
        }
    }
}
