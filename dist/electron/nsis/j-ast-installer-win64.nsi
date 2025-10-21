;-------------------------------------------------------------------------------
; Includes
!include "MUI2.nsh"
!include "LogicLib.nsh"
!include "WinVer.nsh"
!include "x64.nsh"

;-------------------------------------------------------------------------------
; Constants
!define PRODUCT_NAME "J-AST"
!define PRODUCT_DESCRIPTION "Analysis tool for AST"
!define COPYRIGHT "Copyright © 2025 HKI Jena"
!define PRODUCT_VERSION "%%JAST_VERSION%%"
!define PRODUCT_VERSION_SHORT "%%JAST_VERSION%%"
!define SETUP_VERSION 1.0.0.0

;-------------------------------------------------------------------------------
; Attributes
Name "J-AST"
OutFile "j-ast-${PRODUCT_VERSION}-windows-x64-installer.exe"
InstallDir "$LocalAppData\${PRODUCT_NAME}-${PRODUCT_VERSION}"
InstallDirRegKey HKCU "Software\HKIJena\${PRODUCT_NAME}-${PRODUCT_VERSION}" "InstallLocation"
RequestExecutionLevel user ; user|highest|admin

;-------------------------------------------------------------------------------
; Version Info
VIProductVersion "${PRODUCT_VERSION}"
VIAddVersionKey "ProductName" "${PRODUCT_NAME}"
VIAddVersionKey "ProductVersion" "${PRODUCT_VERSION}"
VIAddVersionKey "FileDescription" "${PRODUCT_DESCRIPTION}"
VIAddVersionKey "LegalCopyright" "${COPYRIGHT}"
VIAddVersionKey "FileVersion" "${SETUP_VERSION}"

;-------------------------------------------------------------------------------
; Modern UI Appearance
!define MUI_ICON "icon.ico"
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_BITMAP "header.bmp"
!define MUI_WELCOMEFINISHPAGE_BITMAP "wizard.bmp"
!define MUI_FINISHPAGE_NOAUTOCLOSE

;-------------------------------------------------------------------------------
; Installer Pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE "LICENSE.txt"
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

;-------------------------------------------------------------------------------
; Uninstaller Pages
!insertmacro MUI_UNPAGE_WELCOME
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_UNPAGE_FINISH

;-------------------------------------------------------------------------------
; Languages
!insertmacro MUI_LANGUAGE "English"

;-------------------------------------------------------------------------------
; Installer Sections
Section "-Application" 

    ; Copy data
	SetOutPath $INSTDIR
	File /r "j-ast-windows-x64\*"

    ; Start menu
    CreateDirectory "$SMPROGRAMS\${PRODUCT_NAME}\${PRODUCT_VERSION_SHORT}"
    CreateShortcut "$SMPROGRAMS\${PRODUCT_NAME}\${PRODUCT_VERSION_SHORT}\Uninstall ${PRODUCT_NAME} ${PRODUCT_VERSION_SHORT}.lnk" "$INSTDIR\Uninstall.exe"
    CreateShortcut "$SMPROGRAMS\${PRODUCT_NAME}\${PRODUCT_VERSION_SHORT}\${PRODUCT_NAME} ${PRODUCT_VERSION_SHORT}.lnk" "$INSTDIR\J-AST.exe" "" "$INSTDIR\j-ast-icon.ico"

SectionEnd

Section "Create desktop icon"
    CreateShortcut "$DESKTOP\${PRODUCT_NAME} ${PRODUCT_VERSION_SHORT}.lnk" "$INSTDIR\J-AST.exe" "" "$INSTDIR\j-ast-icon.ico"
SectionEnd 

Section "-PostInstall"
    WriteUninstaller "$INSTDIR\Uninstall.exe"

    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}-${PRODUCT_VERSION}" "DisplayName" "${PRODUCT_NAME} ${PRODUCT_VERSION_SHORT}"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}-${PRODUCT_VERSION}" "UninstallString" "$INSTDIR\Uninstall.exe"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}-${PRODUCT_VERSION}" "InstallLocation" "$INSTDIR"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}-${PRODUCT_VERSION}" "DisplayIcon" "$INSTDIR\j-ast-icon.ico"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}-${PRODUCT_VERSION}" "Publisher" "HKI Jena"
    WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}-${PRODUCT_VERSION}" "DisplayVersion" "${PRODUCT_VERSION_SHORT}"
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}-${PRODUCT_VERSION}" "NoModify" 1
    WriteRegDWORD HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}-${PRODUCT_VERSION}" "NoRepair" 1
SectionEnd

;-------------------------------------------------------------------------------
; Uninstaller Sections
Section "-Uninstall"
	RMDir /r "$INSTDIR"

    ; Start menu
    RMDir /r "$SMPROGRAMS\${PRODUCT_NAME}\${PRODUCT_VERSION_SHORT}"
    RMDir "$SMPROGRAMS\${PRODUCT_NAME}"

    ; Desktop shortcuts
    Delete "$DESKTOP\${PRODUCT_NAME} ${PRODUCT_VERSION_SHORT}.lnk"
    Delete "$DESKTOP\ImageJ (${PRODUCT_NAME} ${PRODUCT_VERSION_SHORT}).lnk"

    ; Remove the registry entry
    DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}-${PRODUCT_VERSION}"
SectionEnd

