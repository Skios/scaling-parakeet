#!/bin/bash
set -e

echo "[wildfly-init] Inizio copia configurazioni e moduli..."

# Copia standalone configuration (come prima)
if [ ! -f /mnt/standalone/configuration/standalone.xml ]; then
    echo "→ Copia configuration standalone"
    mkdir -p /mnt/standalone/configuration
    cp -r /opt/jboss/wildfly/standalone/configuration/* /mnt/standalone/configuration/
    sync
    sleep 1
else
    echo "→ Configuration standalone già presente, skip"
fi

# Copia domain configuration (come prima)
if [ ! -f /mnt/domain/configuration/domain.xml ]; then
    echo "→ Copia configuration domain"
    mkdir -p /mnt/domain/configuration
    cp -r /opt/jboss/wildfly/domain/configuration/* /mnt/domain/configuration/
    sync
    sleep 1
else
    echo "→ Configuration domain già presente, skip"
fi

# MODULES - Usa script dedicato con debug esteso
if [ ! -d /mnt/modules/system ]; then
    echo ""
    echo "🚨 INIZIO COPIA MODULES CON DEBUG ESTESO"
    echo "=========================================="

    # Salva il script di debug inline
    cat > /tmp/copy-modules-debug.sh << 'SCRIPT_EOF'
#!/bin/bash
set -e

echo "🔍 DEBUG: Analisi situazione modules..."

# Debug informazioni iniziali
echo "📊 Mount point /mnt/modules:"
mount | grep /mnt/modules || echo "   Nessun mount specifico trovato"
df -h /mnt/modules || echo "   Impossibile vedere disk usage"

echo ""
echo "📊 Source directory /opt/jboss/wildfly/modules:"
ls -la /opt/jboss/wildfly/modules/ || echo "   Source non accessibile"
du -sh /opt/jboss/wildfly/modules 2>/dev/null || echo "   Impossibile calcolare dimensione source"

echo ""
echo "📊 Target directory /mnt/modules:"
ls -la /mnt/modules/ || echo "   Target non accessibile"
du -sh /mnt/modules 2>/dev/null || echo "   Target vuoto o inaccessibile"

# Test preliminare di scrittura
echo ""
echo "🧪 Test di scrittura su /mnt/modules..."
if touch /mnt/modules/test_write 2>/dev/null; then
    echo "✅ Scrittura possibile"
    rm -f /mnt/modules/test_write
else
    echo "❌ PROBLEMA: Impossibile scrivere su /mnt/modules"
    echo "   Verifica permessi del volume mount"
    exit 1
fi

# Metodo 1: Copia diretta con verbose
echo ""
echo "🔄 Metodo 1: Tentativo copia diretta..."
mkdir -p /mnt/modules

# Copia con output dettagliato
if timeout 300 cp -rv /opt/jboss/wildfly/modules/* /mnt/modules/ 2>&1 | tail -20; then
    echo "✅ Copia diretta completata"
    sync
    echo "🔄 Sync completato, attesa 5 secondi..."
    sleep 5
else
    echo "⚠️ Copia diretta fallita o timeout, provo tar..."

    # Metodo 2: Tar con timeout
    echo ""
    echo "🔄 Metodo 2: Tar con timeout..."
    cd /opt/jboss/wildfly

    if timeout 300 bash -c "tar cf - modules | (cd /mnt && tar xf -)"; then
        echo "✅ Tar completato"
        sync
        sleep 5
    else
        echo "❌ Anche tar ha fallito/timeout"
        exit 1
    fi
fi

# Verifica finale
echo ""
echo "🔍 VERIFICA FINALE:"
if [ -d "/mnt/modules/system" ]; then
    echo "✅ Directory system presente"
    jar_count=$(find /mnt/modules -name "*.jar" 2>/dev/null | wc -l)
    echo "📦 JAR trovati: $jar_count"
else
    echo "❌ Directory system mancante"
    echo "Contenuto /mnt/modules:"
    ls -la /mnt/modules/ 2>/dev/null || echo "Vuoto"
    exit 1
fi
SCRIPT_EOF

    chmod +x /tmp/copy-modules-debug.sh

    # Esegui lo script di debug
    if /tmp/copy-modules-debug.sh; then
        echo "✅ MODULES COPIATI CON SUCCESSO"
    else
        echo "❌ ERRORE NELLA COPIA MODULES"
        exit 1
    fi

    echo "=========================================="
    echo "🚨 FINE COPIA MODULES"
    echo ""
else
    echo "→ Modules già presenti, skip"
fi

echo "[wildfly-init] Fine copia. Init completato."

# Attesa finale più lunga
echo "⏳ Attesa finale estesa per sync completo... 15 secondi"
sync
sleep 15

echo ""
echo "[DEBUG FINALE] Riepilogo:"
echo "- Standalone config: $([ -f /mnt/standalone/configuration/standalone.xml ] && echo "✅" || echo "❌")"
echo "- Domain config: $([ -f /mnt/domain/configuration/domain.xml ] && echo "✅" || echo "❌")"
echo "- Modules system: $([ -d /mnt/modules/system ] && echo "✅" || echo "❌")"

if [ -d "/mnt/modules/system" ]; then
    echo "- JAR count: $(find /mnt/modules -name "*.jar" 2>/dev/null | wc -l)"
    echo "- Total size: $(du -sh /mnt/modules 2>/dev/null | cut -f1)"
fi