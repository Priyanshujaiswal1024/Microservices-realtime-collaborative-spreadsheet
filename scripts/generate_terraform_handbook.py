import os
import sys
from reportlab.lib.pagesizes import letter
from reportlab.lib import colors
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether, HRFlowable
)
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.pdfgen import canvas

class NumberedCanvas(canvas.Canvas):
    """
    Two-pass canvas to dynamically compute total pages and draw headers/footers.
    """
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self._saved_page_states = []

    def showPage(self):
        self._saved_page_states.append(dict(self.__dict__))
        self._startPage()

    def save(self):
        num_pages = len(self._saved_page_states)
        for state in self._saved_page_states:
            self.__dict__.update(state)
            self.draw_header_footer(num_pages)
            super().showPage()
        super().save()

    def draw_header_footer(self, page_count):
        self.saveState()
        self.setFont("Helvetica", 8)
        self.setFillColor(colors.HexColor("#718096"))
        
        # Omit header/footer on cover page (page 1)
        if self._pageNumber > 1:
            # Header
            self.drawString(54, 750, "TERRAFORM ENTERPRISE & DEVOPS INTERVIEW MASTER HANDBOOK")
            self.drawRightString(558, 750, "CANDIDATE: PRIYANSHU JAISWAL")
            self.setStrokeColor(colors.HexColor("#CBD5E0"))
            self.setLineWidth(0.5)
            self.line(54, 742, 558, 742)
            
            # Footer
            self.setStrokeColor(colors.HexColor("#CBD5E0"))
            self.setLineWidth(0.5)
            self.line(54, 45, 558, 45)
            self.drawString(54, 32, "Target: Deloitte | EY | HCLTech | Product & Cloud Consulting")
            page_text = f"Page {self._pageNumber} of {page_count}"
            self.drawRightString(558, 32, page_text)
            
        self.restoreState()

def create_handbook(output_path):
    doc = SimpleDocTemplate(
        output_path,
        pagesize=letter,
        leftMargin=54,
        rightMargin=54,
        topMargin=54,
        bottomMargin=54
    )

    styles = getSampleStyleSheet()
    
    # Custom Color Palette
    PRIMARY = colors.HexColor("#1A365D")   # Deep Navy
    SECONDARY = colors.HexColor("#2B6CB0") # Slate Blue
    ACCENT = colors.HexColor("#C05621")    # Amber / Terracotta
    DARK_TEXT = colors.HexColor("#2D3748") # Charcoal
    CODE_BG = colors.HexColor("#EDF2F7")   # Light Gray
    CARD_BG = colors.HexColor("#F7FAFC")   # Ultra light
    QUOTE_BG = colors.HexColor("#EBF8FF")  # Soft blue tint
    BORDER_COL = colors.HexColor("#CBD5E0")

    # Typography Styles
    title_style = ParagraphStyle(
        'DocTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=24,
        leading=30,
        textColor=PRIMARY,
        alignment=1, # Center
        spaceAfter=8
    )
    
    subtitle_style = ParagraphStyle(
        'DocSubtitle',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=12,
        leading=16,
        textColor=SECONDARY,
        alignment=1,
        spaceAfter=15
    )

    h1_style = ParagraphStyle(
        'SectionH1',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=15,
        leading=19,
        textColor=PRIMARY,
        spaceBefore=14,
        spaceAfter=8,
        keepWithNext=True
    )

    q_num_style = ParagraphStyle(
        'QNumber',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=11,
        leading=15,
        textColor=ACCENT,
        spaceBefore=10,
        spaceAfter=2,
        keepWithNext=True
    )

    q_title_style = ParagraphStyle(
        'QTitle',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=12,
        leading=16,
        textColor=PRIMARY,
        spaceAfter=6,
        keepWithNext=True
    )

    body_style = ParagraphStyle(
        'MainBody',
        parent=styles['Normal'],
        fontName='Helvetica',
        fontSize=9.5,
        leading=13.5,
        textColor=DARK_TEXT,
        spaceAfter=6
    )

    spoken_style = ParagraphStyle(
        'SpokenEnglish',
        parent=styles['Normal'],
        fontName='Helvetica-Oblique',
        fontSize=9,
        leading=13,
        textColor=colors.HexColor("#2C5282")
    )

    code_style = ParagraphStyle(
        'CodeStyle',
        parent=styles['Normal'],
        fontName='Courier',
        fontSize=8,
        leading=10.5,
        textColor=colors.HexColor("#1A202C")
    )

    badge_style = ParagraphStyle(
        'Badge',
        parent=styles['Normal'],
        fontName='Helvetica-Bold',
        fontSize=8,
        leading=10,
        textColor=colors.white
    )

    story = []

    # ==================== COVER / HEADER BANNER ====================
    story.append(Spacer(1, 10))
    story.append(Paragraph("TERRAFORM MASTER INTERVIEW HANDBOOK", title_style))
    story.append(Paragraph("From Zero to Enterprise DevOps | Comprehensive Q&A, Architecture, Scenarios & Spoken English Answers", subtitle_style))
    
    meta_info = [
        [
            Paragraph("<b>Candidate:</b> Priyanshu Jaiswal", body_style),
            Paragraph("<b>Focus:</b> Full Stack Java & Cloud DevOps", body_style)
        ],
        [
            Paragraph("<b>Target Companies:</b> Deloitte, EY, HCLTech, Product Firms", body_style),
            Paragraph("<b>Edition:</b> 2026 Enterprise Edition", body_style)
        ]
    ]
    meta_table = Table(meta_info, colWidths=[250, 254])
    meta_table.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), CARD_BG),
        ('BOX', (0, 0), (-1, -1), 1, BORDER_COL),
        ('INNERGRID', (0, 0), (-1, -1), 0.5, BORDER_COL),
        ('TOPPADDING', (0, 0), (-1, -1), 6),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
        ('LEFTPADDING', (0, 0), (-1, -1), 10),
        ('RIGHTPADDING', (0, 0), (-1, -1), 10),
    ]))
    story.append(meta_table)
    story.append(Spacer(1, 15))
    story.append(HRFlowable(width="100%", thickness=1.5, color=PRIMARY, spaceBefore=4, spaceAfter=14))

    # Helper function to add a Q&A Card
    def add_qa(q_num, q_title, why_text, answer_text, spoken_text, code_snippet=None):
        qa_elements = []
        qa_elements.append(Paragraph(f"QUESTION {q_num}", q_num_style))
        qa_elements.append(Paragraph(q_title, q_title_style))
        
        # The "Why" Context
        if why_text:
            qa_elements.append(Paragraph(f"<b>Context & Why it's asked:</b> {why_text}", body_style))
        
        # Detailed Answer
        qa_elements.append(Paragraph(f"<b>Detailed Technical Answer:</b> {answer_text}", body_style))
        
        # Code Snippet if provided
        if code_snippet:
            code_p = Paragraph(code_snippet.replace("\n", "<br/>").replace(" ", "&nbsp;"), code_style)
            code_table = Table([[code_p]], colWidths=[504])
            code_table.setStyle(TableStyle([
                ('BACKGROUND', (0, 0), (-1, -1), CODE_BG),
                ('BOX', (0, 0), (-1, -1), 0.5, BORDER_COL),
                ('TOPPADDING', (0, 0), (-1, -1), 6),
                ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
                ('LEFTPADDING', (0, 0), (-1, -1), 8),
                ('RIGHTPADDING', (0, 0), (-1, -1), 8),
            ]))
            qa_elements.append(Spacer(1, 3))
            qa_elements.append(code_table)
            qa_elements.append(Spacer(1, 4))
        
        # Spoken English Box
        if spoken_text:
            spoken_p = Paragraph(f"<b>🗣️ Spoken English Interview Soundbite:</b><br/>\"{spoken_text}\"", spoken_style)
            spoken_table = Table([[spoken_p]], colWidths=[504])
            spoken_table.setStyle(TableStyle([
                ('BACKGROUND', (0, 0), (-1, -1), QUOTE_BG),
                ('LINELEFT', (0, 0), (0, -1), 3, SECONDARY),
                ('BOX', (0, 0), (-1, -1), 0.5, colors.HexColor("#BEE3F8")),
                ('TOPPADDING', (0, 0), (-1, -1), 6),
                ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
                ('LEFTPADDING', (0, 0), (-1, -1), 10),
                ('RIGHTPADDING', (0, 0), (-1, -1), 8),
            ]))
            qa_elements.append(Spacer(1, 3))
            qa_elements.append(spoken_table)
            
        qa_elements.append(Spacer(1, 10))
        qa_elements.append(HRFlowable(width="100%", thickness=0.5, color=colors.HexColor("#E2E8F0"), spaceBefore=2, spaceAfter=8))
        
        story.append(KeepTogether(qa_elements))

    # ==================== SECTION 1 ====================
    story.append(Paragraph("SECTION 1: CORE IAC & TERRAFORM FUNDAMENTALS", h1_style))
    story.append(HRFlowable(width="100%", thickness=1, color=SECONDARY, spaceBefore=2, spaceAfter=10))

    add_qa(
        1,
        "What is Infrastructure as Code (IaC) and what core problems does it solve?",
        "Checks your understanding of why modern cloud engineering moved away from manual AWS Web Console clicking.",
        "Infrastructure as Code is the practice of provisioning and managing cloud infrastructure using machine-readable configuration definition files rather than manual physical hardware configuration or interactive web consoles. It eliminates human error, configuration drift, and documentation rot while introducing version control, automated peer reviews, rollbacks, and fast environment replication.",
        "IaC transforms infrastructure from a manual, error-prone clicking process into version-controlled software. By defining infrastructure in code, we achieve automated provisioning, auditability through Git commits, and zero drift across Dev, Stage, and Production environments."
    )

    add_qa(
        2,
        "How does Terraform's declarative model differ from imperative configuration tools like Ansible or Bash?",
        "Tests if you understand state convergence and declarative vs imperative paradigms.",
        "In an imperative model (like Bash or Ansible playbooks), you define the exact sequence of operational steps: 'create VM, install package, start service'. If the script runs twice, it might fail unless manually made idempotent.<br/>In Terraform's declarative model, you declare the desired end-state ('I want 3 EC2 instances in us-east-1'). Terraform evaluates current state against desired state and automatically calculates the minimum delta required to reach that target.",
        "The core difference is 'What' versus 'How'. Imperative tools require you to specify every step to execute, whereas Terraform is declarative: we simply specify our desired end-state, and Terraform's graph engine determines the exact API lifecycle steps required to achieve that state."
    )

    add_qa(
        3,
        "Explain the core Terraform CLI lifecycle: init, plan, apply, and destroy.",
        "The foundational workflow question asked in every entry-level and mid-level DevOps interview.",
        "<b>1. terraform init:</b> Initializes the working directory, downloads required provider plugins into .terraform/, initializes the remote backend, and resolves modules.<br/><b>2. terraform plan:</b> Performs an execution dry-run. It refreshes current state from cloud APIs, calculates differences against code, and outputs an execution plan (+ create, ~ update, - destroy) without making real changes.<br/><b>3. terraform apply:</b> Executes the planned changes against provider APIs to provision or alter infrastructure and updates the state file.<br/><b>4. terraform destroy:</b> Reverses the dependency graph and safely terminates all resources managed within that state file.",
        "The Terraform workflow follows a disciplined lifecycle: init prepares the workspace and providers; plan performs an idempotent dry-run showing expected deltas; apply provisions the changes to cloud APIs; and destroy tears down the managed infrastructure when no longer needed.",
        "terraform init\nterraform plan -out=tfplan\nterraform apply tfplan\nterraform destroy"
    )

    add_qa(
        4,
        "What is Idempotency in Terraform and why is it critical for enterprise reliability?",
        "A critical systems engineering concept asked by technical leads.",
        "An operation is idempotent if executing it multiple times produces the exact same result without unintended side effects. In Terraform, if you run 'terraform apply' on an infrastructure that already matches your code, Terraform does nothing: 'No changes. Your infrastructure matches the configuration.' This guarantees that running automated CI/CD pipelines repeatedly will never accidentally spin up duplicate instances or corrupt live data.",
        "Idempotency means running the same configuration multiple times yields the exact same outcome without duplicate resource creation. In production, idempotency prevents accidental resource duplication and ensures our CI/CD pipelines are safe to execute continuously."
    )

    # ==================== SECTION 2 ====================
    story.append(Spacer(1, 10))
    story.append(Paragraph("SECTION 2: VARIABLES, OUTPUTS & CONFIGURATION PRECEDENCE", h1_style))
    story.append(HRFlowable(width="100%", thickness=1, color=SECONDARY, spaceBefore=2, spaceAfter=10))

    add_qa(
        5,
        "What is the exact variable definition precedence order in Terraform?",
        "One of the most frequently asked tricky questions to test practical debugging skills.",
        "When a variable is assigned values in multiple places, Terraform resolves them in this strict order (highest priority wins):<br/>"
        "1. <b>-var and -var-file</b> command-line flags (Highest priority)<br/>"
        "2. <b>*.auto.tfvars</b> or <b>*.auto.tfvars.json</b> files (alphabetical order)<br/>"
        "3. <b>terraform.tfvars.json</b> file<br/>"
        "4. <b>terraform.tfvars</b> file<br/>"
        "5. <b>TF_VAR_variable_name</b> environment variables<br/>"
        "6. <b>default</b> value declared inside the variable block (Lowest priority)",
        "Terraform evaluates variable precedence from lowest to highest: default block values are overridden by TF_VAR environment variables, which are overridden by terraform.tfvars, auto.tfvars, and finally the command-line -var flag, which always takes highest precedence."
    )

    add_qa(
        6,
        "How do you implement input variable validation to prevent invalid infrastructure provisioning?",
        "Distinguishes senior engineers who write defensive code from juniors who accept any input.",
        "Terraform allows a <b>validation {}</b> block inside variables. It evaluates a boolean condition and returns an error message before making cloud API calls. For instance, restricting instance sizes to free-tier or approved company sizes.",
        "We implement custom validation rules within our variable definitions using the validation block. This ensures parameters conform to organizational compliance—like allowed instance types or naming conventions—failing fast at the plan stage before calling AWS APIs.",
        'variable "instance_type" {\n  type    = string\n  default = "t3.micro"\n  validation {\n    condition     = contains(["t3.micro", "t3.small"], var.instance_type)\n    error_message = "Only t3.micro or t3.small instances are permitted."\n  }\n}'
    )

    # ==================== SECTION 3 ====================
    story.append(Spacer(1, 10))
    story.append(Paragraph("SECTION 3: STATE MANAGEMENT, REMOTE BACKENDS & LOCKING", h1_style))
    story.append(HRFlowable(width="100%", thickness=1, color=SECONDARY, spaceBefore=2, spaceAfter=10))

    add_qa(
        7,
        "What is the Terraform State file and why is storing it in Git an anti-pattern?",
        "Security and concurrency question asked by Deloitte, EY, and enterprise firms.",
        "The state file (terraform.tfstate) is a JSON document mapping your HCL code to physical cloud IDs and metadata. Committing state to Git is dangerous because:<br/>"
        "1. <b>Plain-text Secrets:</b> Database passwords, private keys, and sensitive tokens are written in plain text in the state file.<br/>"
        "2. <b>Concurrency Collisions:</b> If two developers run apply simultaneously, Git merge conflicts can corrupt the state.<br/>"
        "3. <b>Lack of State Locking:</b> Git does not provide distributed locks during execution.",
        "The state file must never be committed to source control for two critical reasons: security and concurrency. State files store sensitive credentials in plain text, and Git lacks the distributed locking mechanism required to prevent concurrent write corruptions across team members."
    )

    add_qa(
        8,
        "How do AWS S3 and DynamoDB work together for production Remote State management?",
        "Standard architecture question for any AWS DevOps engineer.",
        "In production, we configure an S3 remote backend. S3 stores the state file centrally with AES-256 encryption at rest, HTTPS in transit, and bucket versioning enabled for rollback recovery. DynamoDB provides <b>State Locking</b> via a LockID attribute: before any execution, Terraform acquires a lock in DynamoDB; if another engineer runs plan/apply, their process is blocked until the lock is released. This guarantees zero state corruption.",
        "In enterprise AWS setups, we decouple state storage and concurrency management: S3 acts as the encrypted, versioned remote storage backend, while DynamoDB provides distributed locking via state lock IDs to eliminate race conditions between engineers and CI/CD pipelines.",
        'terraform {\n  backend "s3" {\n    bucket         = "company-tf-state-prod"\n    key            = "prod/terraform.tfstate"\n    region         = "us-east-1"\n    dynamodb_table = "terraform-state-locks"\n    encrypt        = true\n  }\n}'
    )

    add_qa(
        9,
        "What happens if a Terraform process crashes leaving DynamoDB locked, and how do you recover?",
        "Real-world incident management question.",
        "If a network connection drops or a CI/CD agent gets killed during apply, the DynamoDB lock remains active. Subsequent runs fail with: 'Error acquiring the state lock: Lock Info: ID...'. To recover, verify no active pipeline is executing, note the Lock ID from the error message, and run <b>terraform force-unlock &lt;LOCK_ID&gt;</b>.",
        "When an apply is forcefully interrupted, the DynamoDB lock can remain orphaned. We resolve this by verifying no pipeline is actively writing, and then executing terraform force-unlock with the specific Lock ID printed in the error console."
    )

    # ==================== SECTION 4 ====================
    story.append(Spacer(1, 10))
    story.append(Paragraph("SECTION 4: STATE CLI OPERATIONS & TERRAFORM IMPORT", h1_style))
    story.append(HRFlowable(width="100%", thickness=1, color=SECONDARY, spaceBefore=2, spaceAfter=10))

    add_qa(
        10,
        "Explain the critical difference between 'terraform destroy' and 'terraform state rm'.",
        "A favorite tricky interview question.",
        "<b>terraform destroy:</b> Deletes the infrastructure from BOTH the state file AND the physical cloud provider (terminates the EC2 instance, drops the database, stops live traffic).<br/>"
        "<b>terraform state rm:</b> Removes the resource tracking ONLY from the state file. It makes ZERO API deletion calls to AWS; the live server or database continues running completely unharmed.",
        "terraform destroy terminates resources from both the state file and the cloud provider, stopping live traffic. In contrast, terraform state rm purely untracks the resource from Terraform state, leaving the cloud infrastructure running completely untouched in AWS."
    )

    add_qa(
        11,
        "What is 'terraform import' and how do you bring legacy, manually created cloud resources into code?",
        "Legacy migration question essential for consulting roles.",
        "When cloud resources were created manually via the AWS console, Terraform is unaware of them. To import them:<br/>"
        "1. Write an empty resource block in main.tf (e.g. resource 'aws_s3_bucket' 'legacy' {}).<br/>"
        "2. Run: <b>terraform import aws_s3_bucket.legacy &lt;actual-bucket-name&gt;</b>.<br/>"
        "3. Run <b>terraform state show aws_s3_bucket.legacy</b> to inspect the imported attributes.<br/>"
        "4. Align the main.tf block with those attributes until 'terraform plan' confirms zero changes.",
        "terraform import allows us to onboard legacy, unmanaged cloud resources without downtime. We define an empty resource skeleton in code, execute terraform import with the resource address and cloud ID, and then backfill the HCL configuration until terraform plan reports zero drift.",
        "# CLI import\nterraform import aws_s3_bucket.legacy my-manual-bucket-name\n\n# Modern Terraform 1.5+ import block\nimport {\n  to = aws_s3_bucket.legacy\n  id = \"my-manual-bucket-name\"\n}"
    )

    add_qa(
        12,
        "How do 'terraform state list', 'state show', and 'state mv' assist in refactoring?",
        "Checks command-line proficiency and refactoring capability.",
        "<b>state list:</b> Enumerates all resource paths currently tracked.<br/>"
        "<b>state show &lt;res&gt;:</b> Displays the complete JSON attribute dump of a single resource.<br/>"
        "<b>state mv &lt;src&gt; &lt;dst&gt;:</b> Renames a resource or moves it into a child module without destroying and recreating it in AWS.",
        "These state inspection commands are critical for Day-2 refactoring: state list gives an active inventory, state show exposes internal attributes, and state mv allows us to rename resources or refactor them into modules with zero cloud downtime."
    )

    # ==================== SECTION 5 ====================
    story.append(Spacer(1, 10))
    story.append(Paragraph("SECTION 5: TERRAFORM MODULES (ARCHITECTURE & REUSABILITY)", h1_style))
    story.append(HRFlowable(width="100%", thickness=1, color=SECONDARY, spaceBefore=2, spaceAfter=10))

    add_qa(
        13,
        "What is a Terraform Module? Explain the relationship between Root Module and Child Module.",
        "Core architectural question.",
        "A module is a container for multiple resources that are used together. It enforces the DRY (Don't Repeat Yourself) principle.<br/>"
        "<b>Root Module:</b> The main directory where you run terraform init, plan, and apply.<br/>"
        "<b>Child Module:</b> Any reusable package/folder called by the root module via a <b>module {}</b> block.",
        "Think of a Terraform module like a class or function in programming: we write the infrastructure logic once inside a child module, and then invoke it from our root module across Dev, Stage, and Prod with parameterized inputs."
    )

    add_qa(
        14,
        "Can a Child Module access Root Module variables directly? Explain the data flow.",
        "Tests understanding of scope isolation.",
        "<b>NO!</b> Module scopes are strictly isolated. A child module cannot see root module variables. Data flow is explicit:<br/>"
        "1. <b>Inputs (Root to Child):</b> Root passes arguments into the module block, which the child module receives in its <b>variables.tf</b>.<br/>"
        "2. <b>Outputs (Child to Root):</b> The child exports results in <b>outputs.tf</b>, which the root accesses via <b>module.&lt;name&gt;.&lt;output_name&gt;</b>.",
        "Module scopes are strictly encapsulated. A child module cannot read root variables directly. Root explicitly passes values into child variables, and receives return values through module outputs."
    )

    add_qa(
        15,
        "What is the Module Renaming Trap and how does the 'moved {}' block solve it?",
        "Senior-level scenario question.",
        "If you rename a module in code (e.g. from module 'web' to module 'frontend'), Terraform evaluates it as: destroy module.web (terminating production servers!) and create module.frontend from scratch. To achieve zero-downtime refactoring without manual state commands, Terraform 1.1+ introduced the <b>moved {}</b> block. Terraform updates the state address automatically without touching the cloud resource.",
        "Renaming a module without migration instructions causes Terraform to destroy the old resource and recreate the new one. The moved block instructs Terraform that the resource was simply relocated in code, updating the state pointer with zero infrastructure downtime.",
        'moved {\n  from = module.web\n  to   = module.frontend\n}'
    )

    # ==================== SECTION 6 ====================
    story.append(Spacer(1, 10))
    story.append(Paragraph("SECTION 6: MULTI-ENVIRONMENT STRATEGIES (WORKSPACES VS DIRECTORIES)", h1_style))
    story.append(HRFlowable(width="100%", thickness=1, color=SECONDARY, spaceBefore=2, spaceAfter=10))

    add_qa(
        16,
        "What are Terraform Workspaces and how does 'terraform.workspace' function?",
        "Understanding workspace state isolation.",
        "Workspaces allow a single configuration directory to maintain multiple distinct state files (e.g. default, dev, prod). Switching workspaces via <b>terraform workspace select dev</b> changes the active state pointer. The built-in <b>terraform.workspace</b> variable allows dynamic parameterization (e.g. instance_type = terraform.workspace == 'prod' ? 't3.large' : 't3.micro').",
        "Workspaces provide state isolation within a single codebase. By switching workspaces, Terraform isolates state files while letting us dynamically interpolate the terraform.workspace string into resource tags and sizes."
    )

    add_qa(
        17,
        "Compare Workspaces vs Directory Isolation with tfvars for Dev/Stage/Prod. Which is preferred?",
        "Deloitte and enterprise cloud architecture favorite.",
        "Workspaces share the exact same backend credentials and codebase. If an engineer runs destroy in the wrong workspace or a bug exists in code, the blast radius is large.<br/>"
        "<b>Directory Isolation</b> (e.g. environments/dev and environments/prod) maintains separate folders pointing to separate AWS accounts and isolated S3 state buckets. <b>In enterprise production, Directory Isolation combined with reusable Modules is the gold standard</b> because it enforces hard security boundaries.",
        "While Workspaces are useful for identical short-lived feature testing, Directory Isolation combined with reusable Modules is the enterprise standard. Directory isolation limits blast radius by binding Dev and Prod to separate AWS accounts and isolated state storage."
    )

    # ==================== SECTION 7 ====================
    story.append(Spacer(1, 10))
    story.append(Paragraph("SECTION 7: DEPENDENCIES, DIRECTED ACYCLIC GRAPH & CYCLE ERRORS", h1_style))
    story.append(HRFlowable(width="100%", thickness=1, color=SECONDARY, spaceBefore=2, spaceAfter=10))

    add_qa(
        18,
        "Explain the difference between Implicit Dependencies and Explicit Dependencies ('depends_on').",
        "Classic Terraform mechanics question.",
        "<b>Implicit Dependency:</b> Formed automatically when Resource B references an attribute of Resource A (e.g. subnet_id = aws_subnet.main.id). Terraform's DAG engine infers that the subnet must be provisioned before the EC2 instance.<br/>"
        "<b>Explicit Dependency (depends_on):</b> Used when two resources have no direct HCL reference, but a real-world runtime dependency exists (e.g. EC2 user_data script downloading a file from S3, or IAM Role policies that must propagate before an instance profile binds).",
        "Implicit dependencies are inferred automatically when one resource references another's attributes. Explicit dependencies, defined via the depends_on meta-argument, are required when resources are decoupled in code but have a strict ordering requirement at runtime.",
        'resource "aws_instance" "web" {\n  ami           = "ami-0c7217cdde317cfec"\n  instance_type = "t3.micro"\n  user_data     = "aws s3 cp s3://app-bucket/app.jar ."\n  depends_on    = [aws_s3_bucket.app_bucket]\n}'
    )

    add_qa(
        19,
        "What causes a Circular Dependency ('Cycle Error') in Terraform and how do you resolve it?",
        "Tests your understanding of Terraform's internal DAG engine.",
        "A Cycle Error occurs when Resource A depends on Resource B, while Resource B simultaneously depends on Resource A. Terraform builds a Directed Acyclic Graph (DAG); cycles create an infinite loop. Resolution: break the cycle by decoupling the shared attribute into a third standalone resource (e.g., using a standalone 'aws_security_group_rule' instead of inline rules inside 'aws_security_group').",
        "A Cycle Error indicates an infinite loop where two resources depend on each other. We resolve this by decoupling the circular attributes into standalone helper resources, such as splitting inline security group rules into independent rule resources."
    )

    # ==================== SECTION 8 ====================
    story.append(Spacer(1, 10))
    story.append(Paragraph("SECTION 8: RESOURCE LIFECYCLE RULES (PRODUCTION ZERO-DOWNTIME)", h1_style))
    story.append(HRFlowable(width="100%", thickness=1, color=SECONDARY, spaceBefore=2, spaceAfter=10))

    add_qa(
        20,
        "What is the default lifecycle of a Terraform resource and why is it dangerous in production?",
        "The fundamental motivation behind lifecycle rules.",
        "The default lifecycle actions are: Create (+), Update in-place (~), Destroy & Recreate (-/+), and Destroy (-).<br/>"
        "The danger lies in <b>Destroy & Recreate (-/+)</b>: by default, Terraform terminates the old resource first, and only then creates the replacement. If updating an AMI, the server is terminated before the new one boots, resulting in 5 to 10 minutes of application downtime.",
        "By default, when a property change forces recreation, Terraform destroys the existing resource before provisioning the replacement. In production, this causes downtime. We override this using the lifecycle block."
    )

    add_qa(
        21,
        "How does 'create_before_destroy = true' achieve Zero-Downtime rolling deployments?",
        "High-frequency interview question.",
        "It reverses the replacement order: Terraform provisions the replacement resource first, confirms it is healthy, updates downstream load balancer targets, and only then terminates the old resource. <b>Caveat:</b> Resources with unique naming constraints will collide; use <b>name_prefix</b> instead of static names.",
        "create_before_destroy flips Terraform's default sequence: it stands up the new resource first, verifies readiness, and then tears down the old one, eliminating availability gaps during server or certificate replacements.",
        'resource "aws_instance" "web" {\n  ami           = "ami-0c7217cdde317cfec"\n  instance_type = "t3.micro"\n  lifecycle {\n    create_before_destroy = true\n  }\n}'
    )

    add_qa(
        22,
        "Explain 'prevent_destroy = true'. What happens if you run 'terraform destroy'?",
        "Mission-critical state protection question.",
        "It acts as an indestructible safety lock on stateful resources (RDS databases, primary S3 buckets). If any execution plan attempts to destroy the resource—whether via 'terraform destroy' or by removing the resource block from code—Terraform halts execution with a fatal error. To genuinely delete it, an engineer must first update code to prevent_destroy = false, apply that change, and then delete.",
        "prevent_destroy is a guardrail against catastrophic accidental deletion. When active, Terraform hard-fails any plan that would destroy the resource. Decommissioning requires a deliberate two-step code change to set the rule to false first."
    )

    add_qa(
        23,
        "How does 'ignore_changes' resolve configuration drift with AWS Auto Scaling or security tools?",
        "Solves the classic 'Terraform fighting Cloud' dilemma.",
        "In production, external systems modify resources (e.g. AWS Auto Scaling scales desired_capacity from 2 to 10, or security scanners append compliance tags). Normally, Terraform detects this as drift and reverts them on every apply. <b>ignore_changes = [desired_capacity, tags]</b> instructs Terraform to manage the core resource while completely ignoring external modifications to those specific attributes.",
        "ignore_changes prevents Terraform from fighting cloud-native automation. We declare dynamic attributes like Auto Scaling desired_capacity or external security tags in the ignore_changes list so Terraform maintains the resource without reverting live operational updates."
    )

    # ==================== SECTION 9 ====================
    story.append(Spacer(1, 10))
    story.append(Paragraph("SECTION 9: PRODUCTION CODE QUALITY, SECURITY & CI/CD GOVERNANCE", h1_style))
    story.append(HRFlowable(width="100%", thickness=1, color=SECONDARY, spaceBefore=2, spaceAfter=10))

    add_qa(
        24,
        "What does 'sensitive = true' achieve, and does it encrypt secrets inside terraform.tfstate?",
        "Critical security question.",
        "Setting sensitive = true on variables and outputs instructs Terraform to redact the value from CLI outputs, terminal screens, and CI/CD log streams, displaying '(sensitive value)' instead. <b>Crucial Gotcha:</b> It does NOT encrypt the value in the state file! The secret is still stored in plain-text JSON in terraform.tfstate, which is why remote S3 encryption (KMS) and strict IAM access policies are mandatory.",
        "sensitive = true redacts credentials from console logs and CI/CD output to prevent credential leakage. However, it does not encrypt values in the state file, which reinforces why remote state encryption and strict S3 access controls are mandatory."
    )

    add_qa(
        25,
        "Compare 'terraform validate', 'TFLint', and 'tfsec / Checkov'. How are they automated in CI/CD?",
        "Distinguishes top-tier DevOps candidates.",
        "<b>terraform validate:</b> Verifies basic HCL syntax and internal references without calling cloud APIs.<br/>"
        "<b>TFLint:</b> A specialized linter that catches provider-specific errors (invalid EC2 instance types, deprecated syntax, naming conventions).<br/>"
        "<b>tfsec / Checkov:</b> Static Application Security Testing (SAST) tools for IaC that scan for security vulnerabilities (open port 22 on 0.0.0.0/0, unencrypted S3 buckets, missing logging).<br/>"
        "In CI/CD, these run as automated Pull Request quality gates before 'terraform plan'.",
        "In our CI/CD pipeline, we run layered quality gates: terraform validate for syntax integrity, TFLint for cloud-specific linting, and tfsec or Checkov to block security misconfigurations before generating execution plans."
    )

    add_qa(
        26,
        "Explain the end-to-end Enterprise Terraform CI/CD pipeline workflow.",
        "The standard system design question for Cloud & DevOps engineers.",
        "1. Developer creates a feature branch and opens a GitHub Pull Request.<br/>"
        "2. GitHub Actions triggers automated checks: <b>terraform fmt -check</b>, <b>validate</b>, and <b>tfsec</b>.<br/>"
        "3. Pipeline runs <b>terraform plan</b> and posts the execution plan diff directly as a comment on the PR.<br/>"
        "4. Tech Lead reviews the plan diff and approves the PR.<br/>"
        "5. On merge to main branch, the production pipeline runs <b>terraform apply -auto-approve</b> in an isolated runner.<br/>"
        "6. Developers are never granted direct write permissions from local laptops.",
        "Our enterprise pipeline enforces GitOps governance: opening a PR triggers automated format, validation, and security scanning, followed by a plan whose diff is posted directly to the PR. Merging to main executes terraform apply through an automated runner, ensuring zero direct cloud writes from developer laptops."
    )

    # ==================== SECTION 10 ====================
    story.append(Spacer(1, 10))
    story.append(Paragraph("SECTION 10: SCENARIO QUESTIONS FOR DELOITTE, EY & HCLTECH", h1_style))
    story.append(HRFlowable(width="100%", thickness=1, color=SECONDARY, spaceBefore=2, spaceAfter=10))

    add_qa(
        27,
        "SCENARIO: 'Our production website experienced a 5-minute outage after an EC2 AMI update. What caused it and how do you resolve it?'",
        "Deloitte and EY production troubleshooting question.",
        "The outage occurred because Terraform's default replacement sequence is Destroy-then-Create (-/+). Updating an AMI cannot be applied in-place, so Terraform terminated the running EC2 instance before the replacement booted and passed health checks. We resolve this by adding <b>lifecycle { create_before_destroy = true }</b> to ensure the replacement instance is fully operational before the old instance is terminated.",
        "This outage occurred because of Terraform's default Destroy-then-Create behavior during AMI replacements. To achieve zero downtime, we implement lifecycle create_before_destroy = true so the replacement instance is healthy and taking traffic before the previous instance is decommissioned."
    )

    add_qa(
        28,
        "SCENARIO: 'How did you architect Terraform for your Real-Time Collaborative Spreadsheet project?'",
        "Tailored specifically for Priyanshu Jaiswal to showcase full-stack cloud mastery.",
        "In the Collaborative Spreadsheet project, manual cloud configuration would lead to configuration drift and reproducibility issues. We architected modular Terraform configurations: a networking module (VPC, public/private subnets, security groups) and a compute module (EC2 instances running containerized Spring Boot services). We enforced lifecycle rules: <b>prevent_destroy</b> on the PostgreSQL database to safeguard sheet cell history, and <b>create_before_destroy</b> on API Gateway/EC2 to preserve live WebSocket STOMP connections during updates.",
        "In my Real-Time Collaborative Spreadsheet project, I automated our AWS infrastructure using modular Terraform. I built reusable modules for networking and compute, secured our PostgreSQL database against accidental deletion using prevent_destroy, and applied create_before_destroy so WebSocket connections never experience downtime during deployments."
    )

    # Build PDF with NumberedCanvas
    doc.build(story, canvasmaker=NumberedCanvas)
    print(f"Handbook successfully created at: {output_path}")

if __name__ == "__main__":
    out_file = sys.argv[1] if len(sys.argv) > 1 else "Terraform_Master_Interview_Handbook.pdf"
    create_handbook(out_file)
