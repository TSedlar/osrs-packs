@GrabResolver(name='jitpack.io', root='https://jitpack.io/')
@Grab(group='com.github.disassemble-io', module='javanalysis', version='93a385a4e0')
import io.disassemble.javanalysis.util.CtClassScanner
import io.disassemble.javanalysis.CtMethodExtensionKt
import io.disassemble.javanalysis.InsnListExtensionKt
import java.nio.file.Files

import static io.disassemble.javanalysis.util.insn.query.InsnQueryKt.*

// Jar already downloaded via wget

// Load gamepack jar into memory
def ctc = new CtClassScanner()
def classes = ctc.scanJar(new File("./gamepack.jar"))

// Find the revision
def revision = findRevision(classes)

// Rename the gamepack
if (revision > 0) {
    def sourceJar = new File("./gamepack.jar").toPath()
    def targetJar = new File("./packs/" + revision + ".jar").toPath()
    if (!targetJar.toFile().exists()) {
        targetJar.toFile().getParentFile().mkdirs()
        Files.copy(sourceJar, targetJar)
        sourceJar.toFile().delete()
        println(revision)
    } else {
        println("Jar for " + revision + " already exists")
    }
} else {
    System.err.println("Failed to find revision")
    System.exit(1)
}

def findRevision(classes) {
    def revision = -1
    if (classes.containsKey("client")) {
        def clientClass = classes["client"]
        def initMethod = clientClass.methods.find { m -> m.name == "init" }
        if (initMethod != null) {
            def insns = CtMethodExtensionKt.getInstructions(initMethod)
            def lines = InsnListExtensionKt.groupByLines(insns)

            def pattern = [
                SIPUSH.operand(765),
                SIPUSH.operand(503),
                SIPUSH.name("revision")
            ]

            lines.any { _, line ->
                def matches = line.find(*pattern)
                if (matches.containsKey("revision")) {
                    revision = matches["revision"].operand
                    return true
                }
            }
        } else {
            System.err.println("Failed to find client#init method")
            System.exit(1)
        }
    } else {
        System.err.println("Failed to find client.class")
        System.exit(1)
    }
    return revision
}
