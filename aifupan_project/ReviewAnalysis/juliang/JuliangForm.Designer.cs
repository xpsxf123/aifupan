namespace juliang
{
    partial class JuliangForm
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.SuspendLayout();
            // 
            // JuliangForm
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 12F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(233, 66);
            this.Name = "JuliangForm";
            this.StartPosition = System.Windows.Forms.FormStartPosition.Manual;
            this.Text = "frmWeb";
            this.Activated += new System.EventHandler(this.frmWeb_Activated);
            this.FormClosing += new System.Windows.Forms.FormClosingEventHandler(this.frmWeb_FormClosing);
            this.Load += new System.EventHandler(this.frmWeb_Load);
            this.ResumeLayout(false);

        }

        #endregion
    }
}