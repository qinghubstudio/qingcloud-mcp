import os
import re

tools_dir = 'src/main/java/com/qingcloud/mcp/xhs/tools'
files = [f for f in os.listdir(tools_dir) if f.endswith('.java')]

java_keywords = {
    'abstract', 'assert', 'boolean', 'break', 'byte', 'case', 'catch', 'char', 'class', 'const',
    'continue', 'default', 'do', 'double', 'else', 'enum', 'extends', 'final', 'finally', 'float',
    'for', 'goto', 'if', 'implements', 'import', 'instanceof', 'int', 'interface', 'long', 'native',
    'new', 'package', 'private', 'protected', 'public', 'return', 'short', 'static', 'strictfp',
    'super', 'switch', 'synchronized', 'this', 'throw', 'throws', 'transient', 'try', 'void',
    'volatile', 'while', 'null', 'true', 'false', 'logger', 'Map', 'List', 'Tool', 'JsonSchema', 'McpServerFeatures', 'Page', 'ObjectMapper', 'LinkedHashMap'
}

for filename in files:
    filepath = os.path.join(tools_dir, filename)
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    new_lines = []
    in_multiline_comment = False
    
    for line in lines:
        stripped = line.strip()
        
        # Check for multiline comment state
        if '/*' in stripped and '*/' not in stripped:
            in_multiline_comment = True
        elif '*/' in stripped:
            in_multiline_comment = False
        
        if not stripped or in_multiline_comment or stripped.startswith('//') or stripped.startswith('/*') or stripped.startswith('*') or stripped.startswith('import') or stripped.startswith('package') or stripped.startswith('}') or stripped.startswith('{'):
            new_lines.append(line)
            continue
            
        # Check if it starts with a keyword or common Java identifier start
        first_word = re.match(r'^(\w+)', stripped)
        if first_word:
            word = first_word.group(1)
            if word in java_keywords or word[0].isupper(): # Capitalized usually means class
                new_lines.append(line)
                continue
                
        # If it doesn't look like code, comment it
        if stripped and not any(stripped.startswith(k) for k in ['@', '+', '.', '(', ')']):
            new_lines.append('// ' + line.lstrip())
        else:
            new_lines.append(line)
            
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)

print("Done fixing files.")
